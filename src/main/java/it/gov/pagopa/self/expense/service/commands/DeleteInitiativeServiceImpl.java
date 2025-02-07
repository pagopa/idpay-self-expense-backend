package it.gov.pagopa.self.expense.service.commands;


import it.gov.pagopa.common.reactive.utils.PerformanceLogger;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.utils.AuditUtilities;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@Slf4j
public class DeleteInitiativeServiceImpl implements DeleteInitiativeService{

    private final ExpenseDataRepository expenseDataRepository;
    private final AuditUtilities auditUtilities;
    private final int pageSize;
    private final long delay;

    public DeleteInitiativeServiceImpl(ExpenseDataRepository expenseDataRepository, AuditUtilities auditUtilities,
                                       @Value("${app.delete.paginationSize}") int pageSize,
                                       @Value("${app.delete.delayTime}") long delay) {
        this.expenseDataRepository = expenseDataRepository;
        this.auditUtilities = auditUtilities;
        this.pageSize = pageSize;
        this.delay = delay;
    }

    @Override
    public Mono<String> execute(String initiativeId) {
        log.info("[DELETE_INITIATIVE] Starting handle delete initiative {}", initiativeId);
        return  execAndLogTiming("DELETE_SELF_EXPENSE", initiativeId, deleteSelfExpense(initiativeId))
                .then(Mono.just(initiativeId));
    }


    private Mono<?> execAndLogTiming(String deleteFlowName, String initiativeId, Mono<?> deleteMono) {
        return PerformanceLogger.logTimingFinally(deleteFlowName, deleteMono, initiativeId);
    }

    private Mono<Void> deleteSelfExpense(String initiativeId) {
        return expenseDataRepository.findByInitiativeIdWithBatch(initiativeId, pageSize)
                .flatMap(of -> expenseDataRepository.deleteById(of.getId())
                        .then(Mono.just(of).delayElement(Duration.ofMillis(delay))), pageSize)
                .doOnNext(expenseData -> auditUtilities.logDeletedExpenseData(expenseData.getUserId(), initiativeId))
                .then()
                .doOnSuccess(i -> log.info("[DELETE_INITIATIVE] Deleted initiative {} from collection: expense_data", initiativeId));
    }


}