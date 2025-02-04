package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class SelfExpenseServiceImpl implements SelfExpenseService{

    private final AnprInfoRepository anprInfoRepository;

    private final ExpenseDataRepository expenseDataRepository;

    private final ExceptionMap exceptionMap;

    public SelfExpenseServiceImpl(AnprInfoRepository anprInfoRepository, ExceptionMap exceptionMap, ExpenseDataRepository expenseDataRepository) {
        this.anprInfoRepository = anprInfoRepository;
        this.expenseDataRepository = expenseDataRepository;
        this.exceptionMap = exceptionMap;
    }

    @Override
    public Mono<ChildResponseDTO> getChildForUserId(String userId, String initiativeId) {

        return anprInfoRepository.findByUserIdAndInitiativeId(userId, initiativeId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.ANPR_INFO_NOT_FOUND,
                                Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)))
                .map(anprInfo -> {
                    ChildResponseDTO childResponseDTO = new ChildResponseDTO();
                    childResponseDTO.setChildList(anprInfo.getChildList());
                    return childResponseDTO;
                });
    }

    @Override
    public Mono<Void> saveExpenseData(ExpenseDataDTO expenseData) {

        return expenseDataRepository.save(ExpenseDataMapper.map(expenseData))
                .then()
                .onErrorResume(e ->
                        Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.EXPENSE_DATA_ERROR_ON_SAVE_DB,
                        Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB)));
    }
}
