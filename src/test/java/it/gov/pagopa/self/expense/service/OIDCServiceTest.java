package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.model.OIDCProviderToken;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.net.URL;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = { OIDCServiceImpl.class })
@TestPropertySource(properties = {
        "app.rest-client.oidc-provider.jwksUrl=http://test.com/jwks",
        "app.rest-client.oidc-provider.issuer=http://test.com/issuer",
        "app.rest-client.oidc-provider.clientId=testClientId"
})
class OIDCServiceTest {

    private static final String ISSUER = "http://test.com/issuer";
    private static final String WRONG_ISSUER = "http://wrong-issuer";
    private static final String CLIENT_ID = "testClientId";
    private static final String WRONG_CLIENT_ID = "wrongTestClientId";
    private static final String FISCAL_CODE = "A123456789";
    private static final String KEY_ID = "keyId";
    private static final int VALIDITY_SECONDS = 3600;  // 1 hour
    private static final int EXPIRED_VALIDITY_SECONDS = -1;  // Expired token

    @Autowired
    private OIDCServiceImpl oidcService;

//    @Test
//    void testValidateTokens_BothValid() throws Exception {
//        JWKSet jwkSet = generateJWKSet();
//        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
//        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
//
//        String idToken = generateIdToken(ISSUER, CLIENT_ID,VALIDITY_SECONDS, jwkSet);
//        String accessToken = generateAccessToken(jwkSet);
//
//        OIDCProviderToken token = new OIDCProviderToken();
//        token.setIdToken(idToken);
//        token.setAccessToken(accessToken);
//
//        assertTrue(oidcService.validateTokens(token));
//
//        jwkSetMockedStatic.close();
//    }
//
//    @Test
//    void testValidateTokens_IdTokenInvalidClientId() throws Exception {
//        JWKSet jwkSet = generateJWKSet();
//        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
//        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
//
//        String idToken = generateIdToken(ISSUER, WRONG_CLIENT_ID, VALIDITY_SECONDS, jwkSet);
//        String accessToken = generateAccessToken(jwkSet);
//
//        OIDCProviderToken token = new OIDCProviderToken();
//        token.setIdToken(idToken);
//        token.setAccessToken(accessToken);
//
//        assertFalse(oidcService.validateTokens(token));
//
//        jwkSetMockedStatic.close();
//    }
//
//
//    @Test
//    void testValidateTokens_IdTokenSignatureNotValid() throws Exception {
//        JWKSet jwkSet = generateJWKSet();
//        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
//        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
//
//        JWKSet jwkSetUsed = generateJWKSet();
//
//        String idToken = generateIdToken(WRONG_ISSUER, CLIENT_ID, VALIDITY_SECONDS, jwkSetUsed);
//        String accessToken = generateAccessToken(jwkSetUsed);
//
//        OIDCProviderToken token = new OIDCProviderToken();
//        token.setIdToken(idToken);
//        token.setAccessToken(accessToken);
//
//        assertFalse(oidcService.validateTokens(token));
//
//        jwkSetMockedStatic.close();
//    }
//
//
    @Test
    void testValidateTokens_IdTokenInvalidIssuer() throws Exception {
        JWKSet jwkSet = generateJWKSet();
        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);

        String idToken = generateIdToken(WRONG_ISSUER, CLIENT_ID, VALIDITY_SECONDS, jwkSet);

        OIDCProviderToken token = new OIDCProviderToken();
        token.setIdToken(idToken);

        assertFalse(oidcService.validateTokens(token));

        jwkSetMockedStatic.close();
    }
//
//    @Test
//    void testValidateTokens_TokenExpired() throws Exception {
//        JWKSet jwkSet = generateJWKSet();
//        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
//        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
//
//        String idToken = generateIdToken(ISSUER, CLIENT_ID,EXPIRED_VALIDITY_SECONDS, jwkSet); // Expired token
//        String accessToken = generateAccessToken(jwkSet);
//
//        OIDCProviderToken token = new OIDCProviderToken();
//        token.setIdToken(idToken);
//        token.setAccessToken(accessToken);
//
//        assertFalse(oidcService.validateTokens(token));
//
//        jwkSetMockedStatic.close();
//    }
//
//    @Test
//    void testValidateTokens_ExceptionThrown() {
//        OIDCProviderToken token = new OIDCProviderToken();
//        token.setAccessToken("valid-access-token");
//        assertFalse(oidcService.validateTokens(token));
//    }
//
//    @Test
//    void testExtractFiscalCodeFromIdToken() throws JOSEException, NoSuchAlgorithmException {
//        JWKSet jwkSet = generateJWKSet();
//        MockedStatic<JWKSet> jwkSetMockedStatic = mockStatic(JWKSet.class);
//        jwkSetMockedStatic.when(() -> JWKSet.load(any(URL.class))).thenReturn(jwkSet);
//
//        String idToken = generateIdToken(ISSUER, CLIENT_ID,VALIDITY_SECONDS, jwkSet);
//
//        String fiscalCode = oidcService.extractFiscalCodeFromIdToken(idToken);
//        assertEquals(FISCAL_CODE, fiscalCode);
//
//        jwkSetMockedStatic.close();
//    }
//
//

    private static String generateIdToken(String issuer,String clientId, int validitySeconds, JWKSet jwkSet) throws JOSEException {
        RSAPrivateKey privateKey = jwkSet.getKeys().get(0).toRSAKey().toRSAPrivateKey();

        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(clientId)
                .subject("1234567890")
                .claim("fiscal_code", FISCAL_CODE)
                .issueTime(new Date())
                .expirationTime(new Date(System.currentTimeMillis() + validitySeconds * 1000L))
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(KEY_ID)
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);

        return signedJWT.serialize();
    }



    private JWKSet generateJWKSet() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();

        JWK rsaJWK = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(KEY_ID)
                .build();

        return new JWKSet(rsaJWK);
    }
}
