package it.gov.pagopa.self.expense.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.SignedJWT;
import it.gov.pagopa.self.expense.model.OIDCProviderToken;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.util.Date;

@Slf4j
@Service
public class OIDCServiceImpl implements OIDCService {

    private final String jwksUrl;
    private final String issuer;
    private final String clientId;

    public OIDCServiceImpl(@Value("${app.rest-client.oidc-provider.jwksUrl}") String jwksUrl,
                           @Value("${app.rest-client.oidc-provider.issuer}") String issuer,
                           @Value("${app.rest-client.oidc-provider.clientId}") String clientId) {
        this.jwksUrl = jwksUrl;
        this.issuer = issuer;
        this.clientId = clientId;
    }

    public boolean validateTokens(OIDCProviderToken oidcToken) {
        try {
            if (!validateToken(oidcToken.getIdToken())) {
                return false;
            }
            return validateToken(oidcToken.getAccessToken());
        } catch (Exception e) {
            log.info(e.getMessage());
            return false;
        }
    }

    private boolean validateToken(String token) throws ParseException, IOException, JOSEException {
        SignedJWT signedJWT = SignedJWT.parse(token);

        JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));

        JWSVerifier verifier = new RSASSAVerifier(jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID()).toRSAKey());
        if (!signedJWT.verify(verifier)) {
            return false;
        }

        if (!signedJWT.getJWTClaimsSet().getIssuer().equals(issuer)) {
            return false;
        }

        if (!signedJWT.getJWTClaimsSet().getAudience().contains(clientId)) {
            return false;
        }

        return !signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());
    }

    public String extractFiscalCodeFromIdToken(String idToken) {
        DecodedJWT decodedJWT = JWT.decode(idToken);
        return decodedJWT.getClaim("fiscal_code").asString();
    }

}
