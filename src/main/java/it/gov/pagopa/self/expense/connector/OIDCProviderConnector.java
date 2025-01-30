package it.gov.pagopa.self.expense.connector;


import it.gov.pagopa.self.expense.model.OIDCProviderToken;
import reactor.core.publisher.Mono;

public interface OIDCProviderConnector {
    Mono<OIDCProviderToken> token(String authCode, String redirectUri);
}

