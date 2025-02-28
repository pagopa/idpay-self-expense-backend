package it.gov.pagopa.self.expense.service;

import com.google.common.io.ByteSource;
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
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

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
    public Mono<Void> saveExpenseData(List<FilePart> fileList, ExpenseDataDTO expenseData) {
        log.info("[SELF-EXPENSE-SERVICE][SAVE] Saving expense data for user: {}", expenseData.getFiscalCode());

        return Flux.fromIterable(fileList)
                .flatMap(file -> fileValidation(file)
                        .flatMap(isValid -> {
                            if (Boolean.FALSE.equals(isValid)) {
                                return Mono.error(exceptionMap.throwException(
                                        Constants.ExceptionName.EXPENSE_DATA_FILE_VALIDATION,
                                        Constants.ExceptionMessage.EXPENSE_DATA_FILE_VALIDATION));
                            }
                            return storeFile(expenseData.getFiscalCode(), file)
                                    .onErrorResume(e -> deleteUploadedFiles(expenseData.getFiscalCode(), fileList)
                                            .then(Mono.error(exceptionMap.throwException(
                                                    Constants.ExceptionName.EXPENSE_DATA_FILE_SAVE,
                                                    Constants.ExceptionMessage.EXPENSE_DATA_FILE_SAVE))));
                        }))
                .then(expenseDataRepository.save(ExpenseDataMapper.map(expenseData, fileList)))
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

    public Mono<Boolean> fileValidation(FilePart file) {

        Mono<Boolean> isEmpty = file.content()
                .map(DataBuffer::readableByteCount)
                .reduce(Integer::sum)
                .map(size -> size == 0);

        Mono<Boolean> isValidContentType = Mono.justOrEmpty(file.headers().getContentType())
                .map(contentType -> Constants.CONTENT_TYPES.contains(contentType.toString()))
                .defaultIfEmpty(false);

        return Mono.zip(isEmpty, isValidContentType)
                .map(tuple -> {
                    boolean empty = tuple.getT1();
                    boolean validContentType = tuple.getT2();

                    if (empty) {
                        log.info("[SELF-EXPENSE-SERVICE][FILE-VALIDATION] File is empty");
                    }

                    if (!validContentType) {
                        log.info("[SELF-EXPENSE-SERVICE][FILE-VALIDATION] ContentType not accepted: {}", Objects.requireNonNull(file.headers().getContentType()));
                    }

                    return !empty && validContentType;
                });
    }

    public Mono<Void> storeFile(String fiscalCode, FilePart filePart) {
        Flux<DataBuffer> dataBufferFlux = filePart.content();
        return  DataBufferUtils.join(dataBufferFlux)
                .flatMap(dataBuffer -> {
                        byte[] bytes = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(bytes);
                        DataBufferUtils.release(dataBuffer);
                    try (InputStream inputStreamFile = ByteSource.wrap(bytes).openStream()) {
                        log.info("[SELF-EXPENSE-SERVICE][STORE-FILE] File {} sent to storage", filePart.filename());
                        fileStorageConnector.uploadFile(inputStreamFile, String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, filePart.filename()), Objects.requireNonNull(filePart.headers().getContentType()).toString());
                    } catch (IOException e) {
                        return Mono.error(e);
                    }
                    return Mono.empty();
                });
    }

    private Mono<Void> deleteUploadedFiles(String fiscalCode, List<FilePart> files) {
        return Flux.fromIterable(files)
                .flatMap(file -> Mono.fromRunnable(() -> {
                    try {
                        fileStorageConnector.delete(String.format(Constants.FILE_PATH_TEMPLATE, fiscalCode, file.filename()));
                        log.info("[SELF-EXPENSE-SERVICE][REVERT-UPLOAD]  File {} deleted from storage", file.filename());
                    } catch (Exception e) {
                        log.error("[SELF-EXPENSE-SERVICE][REVERT-UPLOAD]  Failed to delete file {}", file.filename(), e);
                    }
                }))
                .then();
    }

}
