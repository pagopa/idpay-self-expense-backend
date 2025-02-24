package it.gov.pagopa.self.expense.service;



import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import reactor.core.publisher.Mono;


public interface WebviewService {
    Mono<String> login();

    Mono<String> token(String authCode, String state);

    Mono<MilAuthAccessToken>  session(String sessionId);

    Mono<MilAuthAccessToken> mock(String sessionId);
}
