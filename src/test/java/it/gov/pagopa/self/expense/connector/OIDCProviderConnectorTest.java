package it.gov.pagopa.self.expense.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

import static com.github.tomakehurst.wiremock.client.WireMock.*;


@ExtendWith(SpringExtension.class)
class OIDCProviderConnectorTest {

    private WireMockServer wireMockServer;
    private OIDCProviderConnectorImpl oidcProviderConnector;

    @Value("${rest-client.oidc-provider.clientId}")
    private String clientId;

    @Value("${rest-client.oidc-provider.clientSecret}")
    private String clientSecret;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089); // Start WireMock on port 8089
        wireMockServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder();
        oidcProviderConnector = new OIDCProviderConnectorImpl(webClientBuilder,
                "http://localhost:8089", clientId, clientSecret);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }


    @Test
    void testTokenRequest() {
        String basicAuth = buildBasicAuthHeader();

        wireMockServer.stubFor(
                post("/")
                        .withHeader("Authorization", equalTo(basicAuth))
                        .willReturn(aResponse()
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"access_token\":\"mock_token\",\"expires_in\":3600}")
                                .withStatus(200)
                        )
        );

        String authCode = "mockAuthCode";
        String redirectUri = "http://localhost/callback";

        StepVerifier.create(oidcProviderConnector.token(authCode, redirectUri))
                .expectNextMatches(response ->
                        response.getAccessToken().equals("mock_token") &&
                                response.getExpiresIn().equals("3600")
                )
                .expectComplete()
                .verify();
    }

    private String buildBasicAuthHeader() {
        String authString = clientId + ":" + clientSecret;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(authString.getBytes());
    }
}