package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.connector.MilAuthConnectorImpl;
import it.gov.pagopa.self.expense.connector.OIDCProviderConnectorImpl;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import it.gov.pagopa.self.expense.model.OIDCProviderToken;
import it.gov.pagopa.self.expense.configuration.PodInfo;
import it.gov.pagopa.self.expense.configuration.RedirectInfo;
import it.gov.pagopa.self.expense.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        WebviewServiceImpl.class,
        ExceptionMap.class,
        ObjectMapper.class,
        RedirectInfo.class })
@TestPropertySource(properties = {
        "redirect.login-url=http://example.com/login?state=<state>&client_id=<client_id>&redirect_uri=<redirect_uri>",
        "redirect.token-url=http://example.com/token?session-id=<session-id>",
        "redirect.client-id=testClientId",
        "redirect.uri=http://example.com/redirect"
})
class WebviewServiceTest {

    private static MockedStatic<Utils> utilsMockedStatic;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockBean
    private CacheServiceImpl cacheService;
    @Autowired
    private ExceptionMap exceptionMap;
    @MockBean
    private MilAuthConnectorImpl milAuthConnector;
    @MockBean
    private OIDCProviderConnectorImpl oidcProviderConnector;
    @MockBean
    private OIDCServiceImpl oidcService;
    @MockBean
    private PodInfo podInfo;
    @Autowired
    private RedirectInfo redirectInfo;
    @Autowired
    private WebviewServiceImpl webviewService;

    private final String sessionId = "3f2504e0-4f89-41d3-9a0c-0305e82c3301-example-123";
    private final String state = "generatedState";
    private final String fiscalCode = "fiscalCode";
    private final String authCode = "authCode";

    @BeforeEach
    void beforeEach() {
        utilsMockedStatic = mockStatic(Utils.class);
    }

    @AfterEach
    void afterEach() {
        utilsMockedStatic.close();
    }

    private void mockSaveToCache(String key, String value, Mono<Boolean> returnValue) {
        when(cacheService.saveToCache(key, value)).thenReturn(returnValue);
    }

    private void mockGetFromCache(String key, Mono<String> returnValue) {
        when(cacheService.getFromCache(key)).thenReturn(returnValue);
    }

    @Test
    void login_Ok() {
        String sessionRedirectUrl = buildLoginRedirectUrl();

        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(state);
        mockSaveToCache(state, state, Mono.just(true));

        StepVerifier.create(webviewService.login())
                .expectNext(sessionRedirectUrl)
                .verifyComplete();
    }

    @Test
    void login_Fail() {
        mockSaveToCache(state, state, Mono.just(false));
        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(state);

        StepVerifier.create(webviewService.login())
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.LOGIN_FAIL))
                .verify();
    }

    @Test
    void session_Ok() throws JsonProcessingException {
        MilAuthAccessToken token = buildMilAuthAccessToken();

        mockGetFromCache(sessionId, Mono.just(objectMapper.writeValueAsString(token)));

        StepVerifier.create(webviewService.session(sessionId))
                .expectNext(token)
                .verifyComplete();
    }

    @Test
    void session_NotFound() {
        mockGetFromCache(sessionId, Mono.empty());

        StepVerifier.create(webviewService.session(sessionId))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.SESSION_NOT_FOUND))
                .verify();
    }

    @Test
    void session_Fail() {
        mockGetFromCache(sessionId, Mono.just("invalid_json"));

        StepVerifier.create(webviewService.session(sessionId))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.SESSION_FAIL))
                .verify();
    }

    @Test
    void token_Ok() throws JsonProcessingException {
        OIDCProviderToken oidcProviderToken = buildOidcProviderToken();
        MilAuthAccessToken milAuthAccessToken = buildMilAuthAccessToken();

        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(sessionId);
        mockGetFromCache(state, Mono.just("state"));
        mockGetTokens(oidcProviderToken, milAuthAccessToken);
        mockSaveToCache(sessionId, objectMapper.writeValueAsString(milAuthAccessToken), Mono.just(true));
        mockSaveToCache(milAuthAccessToken.getAccessToken(), fiscalCode, Mono.just(true));

        StepVerifier.create(webviewService.token(authCode, state))
                .expectNext(redirectInfo.getTokenRedirect().replace("<session-id>", sessionId))
                .verifyComplete();
    }

    @Test
    void token_StateNotFound() {
        mockGetFromCache(state, Mono.empty());

        StepVerifier.create(webviewService.token(authCode, state))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.STATE_NOT_FOUND))
                .verify();
    }

    @Test
    void token_InvalidOidcToken() {
        OIDCProviderToken oidcProviderToken = buildOidcProviderToken();

        mockGetFromCache(state, Mono.just("state"));
        when(oidcProviderConnector.token(authCode, redirectInfo.getRedirectUri())).thenReturn(Mono.just(oidcProviderToken));
        when(oidcService.validateTokens(any())).thenReturn(false);

        StepVerifier.create(webviewService.token(authCode, state))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.TOKEN_VALIDATION))
                .verify();
    }

    @Test
    void token_UserSaveFail() throws JsonProcessingException {
        OIDCProviderToken oidcProviderToken = buildOidcProviderToken();
        MilAuthAccessToken milAuthAccessToken = buildMilAuthAccessToken();

        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(sessionId);
        mockGetFromCache(state, Mono.just("state"));
        mockGetTokens(oidcProviderToken, milAuthAccessToken);
        mockSaveToCache(sessionId, objectMapper.writeValueAsString(milAuthAccessToken), Mono.just(true));
        mockSaveToCache(milAuthAccessToken.getAccessToken(), fiscalCode, Mono.just(false));

        StepVerifier.create(webviewService.token(authCode, state))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.USER_SAVE_FAIL))
                .verify();
    }

    @Test
    void token_TokenSaveFail() throws JsonProcessingException {
        OIDCProviderToken oidcProviderToken = buildOidcProviderToken();
        MilAuthAccessToken milAuthAccessToken = buildMilAuthAccessToken();

        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(sessionId);
        mockGetFromCache(state, Mono.just("state"));
        mockGetTokens(oidcProviderToken, milAuthAccessToken);
        mockSaveToCache(sessionId, objectMapper.writeValueAsString(milAuthAccessToken), Mono.just(false));


        StepVerifier.create(webviewService.token(authCode, state))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.TOKEN_SAVE_FAIL))
                .verify();


    }

    @Test
    void token_TokenDeserializationFail() throws JsonProcessingException {
        OIDCProviderToken oidcProviderToken = buildOidcProviderToken();
        MilAuthAccessToken milAuthAccessToken = buildMilAuthAccessToken();

        utilsMockedStatic.when(() -> Utils.generateUUID(any())).thenReturn(sessionId);
        mockGetFromCache(state, Mono.just("state"));
        when(oidcProviderConnector.token(authCode, redirectInfo.getRedirectUri())).thenReturn(Mono.just(oidcProviderToken));
        when(oidcService.validateTokens(any())).thenReturn(true);
        when(milAuthConnector.token()).thenReturn(Mono.just(milAuthAccessToken));
        mockSaveToCache(sessionId, objectMapper.writeValueAsString(milAuthAccessToken), Mono.just(false));
        spyObjectMapper(milAuthAccessToken);

        StepVerifier.create(webviewService.token(authCode, state))
                .expectErrorMatches(throwable -> throwable.getMessage().contains(Constants.ExceptionMessage.TOKEN_DESERIALIZATION))
                .verify();

        ReflectionTestUtils.setField(webviewService, "objectMapper", objectMapper);
    }

    private void spyObjectMapper(MilAuthAccessToken milAuthAccessToken) throws JsonProcessingException {
        ObjectMapper spyMapper = Mockito.spy(objectMapper);
        doThrow(new JsonProcessingException("Serialization error"){}).when(spyMapper).writeValueAsString(milAuthAccessToken);
        ReflectionTestUtils.setField(webviewService, "objectMapper", spyMapper);
    }

    private void mockGetTokens(OIDCProviderToken oidcProviderToken, MilAuthAccessToken milAuthAccessToken) {
        when(oidcProviderConnector.token(authCode, redirectInfo.getRedirectUri())).thenReturn(Mono.just(oidcProviderToken));
        when(oidcService.validateTokens(any())).thenReturn(true);
        when(milAuthConnector.token()).thenReturn(Mono.just(milAuthAccessToken));
        when(oidcService.extractFiscalCodeFromIdToken(oidcProviderToken.getIdToken())).thenReturn(fiscalCode);
    }

    private String buildLoginRedirectUrl() {
        return redirectInfo.getLoginRedirect()
                .replace("<state>", "generatedState")
                .replace("<client_id>", redirectInfo.getClientId())
                .replace("<redirect_uri>", redirectInfo.getRedirectUri());
    }

    private MilAuthAccessToken buildMilAuthAccessToken() {
        MilAuthAccessToken token = new MilAuthAccessToken();
        token.setAccessToken("accessToken");
        token.setTokenType("tokenType");
        token.setExpiresIn("expiresIn");
        return token;
    }

    private OIDCProviderToken buildOidcProviderToken() {
        OIDCProviderToken token = new OIDCProviderToken();
        token.setIdToken("idToken");
        token.setAccessToken("accessToken");
        return token;
    }
}
