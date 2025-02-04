package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.ExpenseData;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;


public interface ExpenseDataRepository extends ReactiveMongoRepository<ExpenseData, String> {

}
