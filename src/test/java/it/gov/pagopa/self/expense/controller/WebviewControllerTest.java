package it.gov.pagopa.self.expense.controller;



import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import it.gov.pagopa.self.expense.service.WebviewService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(WebviewController.class)
@TestPropertySource(locations = "classpath:application.yml")
class WebviewControllerTest {
    private static final String DEEP_LINK = "http://example.com/deepLink";
    private static final String AUTH_CODE = "authCode";
    private static final String STATE = "state";
    private static final String SESSION_ID = "3f2504e0-4f89-41d3-9a0c-0305e82c3301-example-123";
    private static final String ACCESS_TOKEN = "accessToken";
    private static final String TOKEN_TYPE = "tokenType";
    private static final String EXPIRES_IN = "expiresIn";

    @MockBean
    private WebviewService webviewService;

    @Autowired
    private WebTestClient webClient;

    @Test
    void login_Ok() {
        Mockito.when(webviewService.login()).thenReturn(Mono.just(DEEP_LINK));

        webClient.get()
                .uri("/idpay/self-expense/login")
                .exchange()
                .expectStatus().isFound()  // Status is HttpStatus.FOUND (302)
                .expectHeader().valueEquals("Location", DEEP_LINK)
                .expectBody().isEmpty();
    }

    @Test
    void token_Ok() {
        Mockito.when(webviewService.token(AUTH_CODE, STATE)).thenReturn(Mono.just(DEEP_LINK));

        webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/idpay/self-expense/redirect")
                        .queryParam("code", AUTH_CODE)
                        .queryParam("state", STATE)
                        .build())
                .exchange()
                .expectStatus().isFound()  // Status is HttpStatus.FOUND (302)
                .expectHeader().valueEquals("Location", DEEP_LINK)
                .expectBody().isEmpty();
    }

    @Test
    void session_Ok() {
        MilAuthAccessToken token = new MilAuthAccessToken();
        token.setAccessToken(ACCESS_TOKEN);
        token.setTokenType(TOKEN_TYPE);
        token.setExpiresIn(EXPIRES_IN);

        Mockito.when(webviewService.session(SESSION_ID)).thenReturn(Mono.just(token));

        webClient.get()
                .uri("/idpay/self-expense/session/{sessionId}", SESSION_ID)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MilAuthAccessToken.class)
                .consumeWith(response -> {
                    MilAuthAccessToken resultResponse = response.getResponseBody();
                    Assertions.assertNotNull(resultResponse);
                    Assertions.assertEquals(token, resultResponse);
                });
    }
}
