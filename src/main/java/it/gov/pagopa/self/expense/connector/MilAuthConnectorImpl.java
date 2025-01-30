package it.gov.pagopa.self.expense.connector;


import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
@Service
public class MilAuthConnectorImpl implements MilAuthConnector {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private static final String GRANT_TYPE = "client_credentials";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";

    public MilAuthConnectorImpl(WebClient.Builder webClientBuilder,
                                @Value("${app.rest-client.mil-auth.baseUrl}") String baseUrl,
                                @Value("${app.rest-client.mil-auth.clientId}") String clientId,
                                @Value("${app.rest-client.mil-auth.clientSecret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<MilAuthAccessToken> token() {
        String body = buildBody();

        return webClient.post()
                .header("Content-Type", CONTENT_TYPE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(MilAuthAccessToken.class);
    }

    private String buildBody() {
        return String.format("grant_type=%s&client_id=%s&client_secret=%s",
                GRANT_TYPE, clientId, clientSecret);
    }
}

