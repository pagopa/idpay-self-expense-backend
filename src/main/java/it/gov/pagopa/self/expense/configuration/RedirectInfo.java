package it.gov.pagopa.self.expense.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class RedirectInfo {

    @Value("${app.redirect.login-url}")
    private String loginRedirect;

    @Value("${app.redirect.token-url}")
    private String tokenRedirect;

    @Value("${app.redirect.client-id}")
    private String clientId;

    @Value("${app.redirect.uri}")
    private String redirectUri;
}