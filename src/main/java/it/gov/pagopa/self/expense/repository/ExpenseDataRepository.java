package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryExt;
import it.gov.pagopa.self.expense.model.ExpenseData;
import reactor.core.publisher.Flux;

public interface ExpenseDataRepository extends ReactiveMongoRepositoryExt<ExpenseData, String>, ExpenseDataRepositoryExt {

    Flux<ExpenseData> findByUserId(String userId);
}
