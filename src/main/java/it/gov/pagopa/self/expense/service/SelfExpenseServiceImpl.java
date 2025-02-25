package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
import it.gov.pagopa.self.expense.model.FileData;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Slf4j
@Service
public class SelfExpenseServiceImpl implements SelfExpenseService {

    private final AnprInfoRepository anprInfoRepository;
    private final ExpenseDataRepository expenseDataRepository;
    private final ExceptionMap exceptionMap;
    private final CacheService cacheService;
    private final UserFiscalCodeService userFiscalCodeService;
    private final RtdProducer rtdProducer;


    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository, ExceptionMap exceptionMap, ExpenseDataRepository expenseDataRepository, CacheService cacheService, UserFiscalCodeService userFiscalCodeService, RtdProducer rtdProducer) {
        this.anprInfoRepository = anprInfoRepository;
        this.expenseDataRepository = expenseDataRepository;
        this.exceptionMap = exceptionMap;
        this.cacheService = cacheService;
        this.userFiscalCodeService = userFiscalCodeService;
        this.rtdProducer = rtdProducer;

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
    public Mono<Void> saveExpenseData(ExpenseDataDTO expenseData) {
        log.info("[SELF-EXPENSE-SERVICE][SAVE] Saving expense data for user: {}", expenseData.getFiscalCode());

        for (FileData fileData : expenseData.getFileList()) {
            byte[] decodedData = CommonUtilities.decodeBase64(new String(fileData.getData(), StandardCharsets.UTF_8));
            fileData.setData(decodedData);
        }

        return expenseDataRepository.save(ExpenseDataMapper.map(expenseData))
                .flatMap(savedData -> rtdProducer.scheduleMessage(expenseData))
                .doOnSuccess(result -> log.info("Expense data saved successfully for user: {}", expenseData.getFiscalCode()))
                .onErrorResume(e -> {
                    log.error("[SELF-EXPENSE-SERVICE][SAVE] Error saving expense data for user: {}", expenseData.getFiscalCode(), e);
                    return Mono.error(exceptionMap.throwException(
                            Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                            Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB));
                });
    }

}
