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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;


@ExtendWith(SpringExtension.class)
class MilAuthConnectorTest {

    private WireMockServer wireMockServer;
    private MilAuthConnectorImpl milAuthConnector;

    @Value("${rest-client.mil-auth.clientId}")
    private String clientId;

    @Value("${rest-client.mil-auth.clientSecret}")
    private String clientSecret;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(8089); // Start WireMock on port 8089
        wireMockServer.start();

        WebClient.Builder webClientBuilder = WebClient.builder();
        milAuthConnector = new MilAuthConnectorImpl(webClientBuilder,
                "http://localhost:8089", clientId, clientSecret);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void testTokenRequest() {
        wireMockServer.stubFor(
                post("/")
                    .willReturn(aResponse()
                            .withHeader("Content-Type", "application/json")
                            .withBody("{\"access_token\":\"mock_token\",\"expires_in\":3600}")
                            .withStatus(200)
                    )
        );
        StepVerifier.create(milAuthConnector.token())
                .expectNextMatches(response ->
                        response.getAccessToken().equals("mock_token") &&
                                response.getExpiresIn().equals("3600")
                )
                .expectComplete()
                .verify();
    }
}
