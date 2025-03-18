package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.ExpenseData;
import reactor.core.publisher.Flux;

public interface ExpenseDataRepositoryExt {


    //ricerca paginata di tutti i doc
    Flux<ExpenseData> findAllWithBatch(int batchSize);
}
