package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.OnboardingFamilies;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;


public interface OnboardingFamiliesRepository extends ReactiveMongoRepository<OnboardingFamilies, String> {

    Flux<OnboardingFamilies> findByInitiativeId(String initiativeId);

}
