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
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Base64;
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

    @Override
    public boolean validateTokens(OIDCProviderToken oidcToken) {
        log.info("[OIDC-SERVICE][VALIDATION] Validating OIDC tokens");
        try {
            if (!validateIdToken(oidcToken.getIdToken())) {
                log.warn("[OIDC-SERVICE][VALIDATION] ID token validation failed");
                return false;
            }
            boolean accessTokenValid = validateAccessToken(oidcToken.getAccessToken(), extractAtHash(oidcToken.getIdToken()));
            if (!accessTokenValid) {
                log.warn("[OIDC-SERVICE][VALIDATION] Access token validation failed");
            }
            return accessTokenValid;
        } catch (Exception e) {
            log.error("[OIDC-SERVICE][VALIDATION] Token validation error: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean validateAccessToken(String accessToken, String hashExpected) throws NoSuchAlgorithmException {
        String hashAlgorithm = "SHA-256";

        byte[] accessTokenBytes = accessToken.getBytes(StandardCharsets.US_ASCII);

        MessageDigest digest = MessageDigest.getInstance(hashAlgorithm);
        byte[] hash = digest.digest(accessTokenBytes);


        byte[] leftHalf = new byte[hash.length / 2];
        System.arraycopy(hash, 0, leftHalf, 0, leftHalf.length);


        String hashActual = Base64.getUrlEncoder().withoutPadding().encodeToString(leftHalf);

        return hashActual.equals(hashExpected);
    }

    private boolean validateIdToken(String token) throws IOException, JOSEException, ParseException {
        log.debug("[OIDC-SERVICE][VALIDATION] Validating token: {}", token);
        SignedJWT signedJWT = SignedJWT.parse(token);


        JWKSet jwkSet = JWKSet.load(new URL(jwksUrl));
        JWSVerifier verifier = new RSASSAVerifier(jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID()).toRSAKey());

        if (!signedJWT.verify(verifier)) {
            log.warn("[OIDC-SERVICE][VALIDATION] Token signature verification failed");
            return false;
        }

        if (!signedJWT.getJWTClaimsSet().getIssuer().equals(issuer)) {
            log.warn("[OIDC-SERVICE][VALIDATION] Token issuer mismatch");
            return false;
        }

        if (!signedJWT.getJWTClaimsSet().getAudience().contains(clientId)) {
            log.warn("[OIDC-SERVICE][VALIDATION] Token audience mismatch");
            return false;
        }

        boolean isTokenExpired = signedJWT.getJWTClaimsSet().getExpirationTime().before(new Date());
        if (isTokenExpired) {
            log.warn("[OIDC-SERVICE][VALIDATION] Token is expired");
        }

        return !isTokenExpired;
    }




    @Override
    public String extractFiscalCodeFromIdToken(String idToken) {
        log.info("[OIDC-SERVICE][EXTRACT] Extracting fiscal code from ID token");
        DecodedJWT decodedJWT = JWT.decode(idToken);
        String fiscalCode = decodedJWT.getClaim("fiscal_code").asString();
        log.debug("[OIDC-SERVICE][EXTRACT] Extracted fiscal code: {}", fiscalCode);
        return fiscalCode;
    }

    @Override
    public String extractAtHash(String idToken) {
        log.info("[OIDC-SERVICE][EXTRACT] Extracting at hash from ID token");
        DecodedJWT decodedJWT = JWT.decode(idToken);
        String atHash = decodedJWT.getClaim("at_hash").asString();
        log.debug("[OIDC-SERVICE][EXTRACT] Extracted at hash: {}", atHash);
        return atHash;
    }
}