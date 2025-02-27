package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.connector.FileStorageConnector;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.util.List;

@Slf4j
@Service
public class SelfExpenseServiceImpl implements SelfExpenseService {

    private final AnprInfoRepository anprInfoRepository;
    private final ExpenseDataRepository expenseDataRepository;
    private final ExceptionMap exceptionMap;
    private final CacheService cacheService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final RtdProducer rtdProducer;

    private final FileStorageConnector fileStorageConnector;
    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository, ExceptionMap exceptionMap, ExpenseDataRepository expenseDataRepository, CacheService cacheService, UserFiscalCodeService userFiscalCodeService, RtdProducer rtdProducer, FileStorageConnector fileStorageConnector) {
        this.anprInfoRepository = anprInfoRepository;
        this.expenseDataRepository = expenseDataRepository;
        this.exceptionMap = exceptionMap;
        this.cacheService = cacheService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.rtdProducer = rtdProducer;
        this.fileStorageConnector = fileStorageConnector;
    }

    @Override
    public Mono<ChildResponseDTO> getChildForUserId(String milAuthToken) {
        log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Fetching child information for user with token: {}", milAuthToken);
        return cacheService.getFromCache(milAuthToken)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                        Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .flatMap(userFiscalCodeService::getUserId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                        Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .flatMap(userId -> {
                    log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Fetching ANPR info for userId: {}", userId);
                    return anprInfoRepository.findByUserId(userId)
                            .switchIfEmpty(Mono.error(exceptionMap.throwException(
                                    Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                                    Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                            .map(anprInfo -> {
                                ChildResponseDTO childResponseDTO = new ChildResponseDTO();
                                childResponseDTO.setChildList(anprInfo.getChildList());
                                childResponseDTO.setUserId(userId);
                                log.info("[SELF-EXPENSE-SERVICE][GET-CHILD] Child information retrieved for userId: {}", userId);
                                return childResponseDTO;
                            });
                });
    }

    @Override
    public Mono<Void> saveExpenseData(List<MultipartFile> files, ExpenseDataDTO expenseData) {
        log.info("[SELF-EXPENSE-SERVICE][SAVE] Saving expense data for user: {}", expenseData.getFiscalCode());

        return Flux.fromIterable(files)
                .flatMap(file -> {
                    if (!fileValidation(file)) {
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.EXPENSE_DATA_FILE_VALIDATION,
                                Constants.ExceptionMessage.EXPENSE_DATA_FILE_VALIDATION));
                    }
                    return storeFile(expenseData.getFiscalCode(), file)
                            .onErrorResume(e -> deleteUploadedFiles(expenseData.getFiscalCode(), files)
                                    .then(Mono.error(exceptionMap.throwException(
                                            Constants.ExceptionName.EXPENSE_DATA_FILE_SAVE,
                                            Constants.ExceptionMessage.EXPENSE_DATA_FILE_SAVE))));
                })
                .then(expenseDataRepository.save(ExpenseDataMapper.map(expenseData, files)))
                .then(userFiscalCodeService.getUserId(expenseData.getFiscalCode())
                        .flatMap(fiscalCode -> rtdProducer.scheduleMessage(expenseData, fiscalCode)))
                .doOnSuccess(result -> log.info("Expense data saved successfully for user: {}", expenseData.getFiscalCode()))
                .onErrorResume(e -> {
                    log.error("[SELF-EXPENSE-SERVICE][SAVE] Error saving expense data for user: {}", expenseData.getFiscalCode(), e);
                    return Mono.error(exceptionMap.throwException(
                            Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                            Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB));
                });
    }

    private boolean fileValidation(MultipartFile file) {
        if (file.isEmpty()) {
            log.info("[SELF-EXPENSE-SERVICE][FILE-VALIDATION] - File is empty");
            return false;
        }
        if (!(Constants.CONTENT_TYPES.contains(file.getContentType()))) {
            log.info("[UPLOAD_FILE_MERCHANT] - ContentType not accepted: {}", file.getContentType());
            return false;
        }
        return true;
    }

    public Mono<Void> storeFile(String fiscalCode, MultipartFile file) {
        return Mono.fromCallable(() -> {
            log.info("[UPLOAD_FILE_MERCHANT] - File {} sent to storage", file.getOriginalFilename());
            InputStream inputStreamFile = file.getInputStream();
            fileStorageConnector.uploadFile(inputStreamFile, String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, file.getOriginalFilename()), file.getContentType());
            return file;
        }).then();
    }

    private Mono<Void> deleteUploadedFiles(String fiscalCode, List<MultipartFile> files) {
        return Flux.fromIterable(files)
                .flatMap(file -> Mono.fromRunnable(() -> {
                    try {
                        fileStorageConnector.delete(String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, file.getOriginalFilename()));
                        log.info("[UPLOAD_FILE_MERCHANT] - File {} deleted from storage", file.getOriginalFilename());
                    } catch (Exception e) {
                        log.error("[UPLOAD_FILE_MERCHANT] - Failed to delete file {}", file.getOriginalFilename(), e);
                    }
                }))
                .then();
    }
}
