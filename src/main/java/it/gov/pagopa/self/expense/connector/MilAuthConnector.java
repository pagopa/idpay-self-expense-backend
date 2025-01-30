package it.gov.pagopa.self.expense.connector;


import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import reactor.core.publisher.Mono;

public interface MilAuthConnector {

    Mono<MilAuthAccessToken> token();
}
