package it.gov.pagopa.self.expense.repository;

import it.gov.pagopa.self.expense.model.AnprInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


public interface AnprInfoRepository extends ReactiveMongoRepository<AnprInfo, String> {

    Mono<AnprInfo> findByUserId(String userId);

    Mono<AnprInfo> findByFamilyId(String familyId);

}
