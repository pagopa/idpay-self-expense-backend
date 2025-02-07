package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.ExpenseData;
import reactor.core.publisher.Flux;

public interface ExpenseDataRepositoryExt {

    Flux<ExpenseData> findByInitiativeIdWithBatch(String initiativeId, int batchSize);
}
