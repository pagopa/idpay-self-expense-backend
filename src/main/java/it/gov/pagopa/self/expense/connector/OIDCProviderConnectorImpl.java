package it.gov.pagopa.self.expense.connector;


import it.gov.pagopa.self.expense.model.OIDCProviderToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class OIDCProviderConnectorImpl implements OIDCProviderConnector {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private static final String GRANT_TYPE = "authorization_code";
    private static final String CONTENT_TYPE = "application/x-www-form-urlencoded";
    private static final String AUTHORIZATION_HEADER_PREFIX = "Basic ";

    public OIDCProviderConnectorImpl(WebClient.Builder webClientBuilder,
                                     @Value("${app.rest-client.oidc-provider.baseUrl}") String baseUrl,
                                     @Value("${app.rest-client.oidc-provider.clientId}") String clientId,
                                     @Value("${app.rest-client.oidc-provider.clientSecret}") String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
    }

    @Override
    public Mono<OIDCProviderToken> token(String authCode, String redirectUri) {
        String basicAuth = buildBasicAuthHeader();

        String body = buildRequestBody(authCode, redirectUri);

        return webClient.post()
                .header("Authorization", basicAuth)
                .header("Content-Type", CONTENT_TYPE)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(OIDCProviderToken.class);
    }

    private String buildBasicAuthHeader() {
        String authString = clientId + ":" + clientSecret;
        return AUTHORIZATION_HEADER_PREFIX + java.util.Base64.getEncoder().encodeToString(authString.getBytes());
    }

    private String buildRequestBody(String authCode, String redirectUri) {
        return String.format("grant_type=%s&code=%s&redirect_url=%s", GRANT_TYPE, authCode, redirectUri);
    }
}


