package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.common.reactive.mongo.ReactiveMongoRepositoryExt;
import it.gov.pagopa.self.expense.model.ExpenseData;



public interface ExpenseDataRepository extends ReactiveMongoRepositoryExt<ExpenseData, String>, ExpenseDataRepositoryExt {



}
