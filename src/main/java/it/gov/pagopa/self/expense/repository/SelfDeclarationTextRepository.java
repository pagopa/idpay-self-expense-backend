package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.SelfDeclarationText;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface SelfDeclarationTextRepository extends ReactiveMongoRepository<SelfDeclarationText, String> {

}
