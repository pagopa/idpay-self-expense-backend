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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
public class WebviewServiceImpl implements WebviewService {

    private final PodInfo podInfo;
    private final CacheServiceImpl cacheService;
    private final ObjectMapper objectMapper;
    private final MilAuthConnectorImpl milAuthConnector;
    private final OIDCProviderConnectorImpl oidcProviderConnector;
    private final OIDCServiceImpl oidcService;
    private final ExceptionMap exceptionMap;
    private final RedirectInfo redirectInfo;

    public WebviewServiceImpl(PodInfo podInfo,
                              CacheServiceImpl cacheService,
                              ObjectMapper objectMapper,
                              MilAuthConnectorImpl milAuthConnector,
                              OIDCProviderConnectorImpl oidcProviderConnector,
                              OIDCServiceImpl oidcService, ExceptionMap exceptionMap, RedirectInfo redirectInfo) {
        this.podInfo = podInfo;
        this.cacheService = cacheService;
        this.objectMapper = objectMapper;
        this.milAuthConnector = milAuthConnector;
        this.oidcProviderConnector = oidcProviderConnector;
        this.oidcService = oidcService;
        this.exceptionMap = exceptionMap;
        this.redirectInfo = redirectInfo;
    }

    @Override
    public Mono<String> login() {
        String state = Utils.generateUUID(podInfo.getPodId());
        log.info("[WEBVIEW-SERVICE][LOGIN] Generated state: {}", state);
        return cacheService.saveToCache(state, state)
                .flatMap(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        String redirectUrl = redirectInfo.getLoginRedirect()
                                .replace("<state>", state)
                                .replace("<client_id>", redirectInfo.getClientId())
                                .replace("<redirect_uri>", redirectInfo.getRedirectUri());
                        log.info("[WEBVIEW-SERVICE][LOGIN] Login redirect URL generated: {}", redirectUrl);
                        return Mono.just(redirectUrl);
                    } else {
                        log.error("[WEBVIEW-SERVICE][LOGIN] Failed to save state to cache");
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.LOGIN_FAIL,
                                Constants.ExceptionMessage.LOGIN_FAIL
                        ));
                    }
                });
    }

    @Override
    public Mono<MilAuthAccessToken> session(String sessionId) {
        log.info("[WEBVIEW-SERVICE][SESSION] Fetching session for sessionId: {}", sessionId);
        return cacheService.getFromCache(sessionId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.SESSION_NOT_FOUND,
                        Constants.ExceptionMessage.SESSION_NOT_FOUND
                )))
                .flatMap(token -> {
                    try {
                        MilAuthAccessToken accessToken = objectMapper.readValue(token, MilAuthAccessToken.class);
                        log.info("[WEBVIEW-SERVICE][SESSION] Session found for sessionId: {}", sessionId);
                        return Mono.just(accessToken);
                    } catch (Exception e) {
                        log.error("[WEBVIEW-SERVICE][SESSION] Error deserializing token for session: {}", sessionId, e);
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.SESSION_FAIL,
                                Constants.ExceptionMessage.SESSION_FAIL
                        ));
                    }
                });
    }

    @Override
    public Mono<String> token(String authCode, String state) {
        log.info("[WEBVIEW-SERVICE][TOKEN] Fetching state from cache: {}", state);
        return cacheService.getFromCache(state)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.STATE_NOT_FOUND,
                        Constants.ExceptionMessage.STATE_NOT_FOUND
                )))
                .flatMap(result -> oidcProviderConnector.token(authCode, redirectInfo.getRedirectUri())
                        .flatMap(this::handleOidcTokenValidation)
                );
    }

    private Mono<String> handleOidcTokenValidation(OIDCProviderToken oidcToken) {
        log.info("[WEBVIEW-SERVICE][TOKEN] Validating OIDC token");
        if (oidcService.validateTokens(oidcToken)) {
            log.info("[WEBVIEW-SERVICE][TOKEN] OIDC token validation successful");
            return milAuthConnector.token()
                    .flatMap(milToken -> generateSessionAndSaveToCache(oidcToken, milToken));
        } else {
            log.error("[WEBVIEW-SERVICE][TOKEN] OIDC token validation failed");
            return Mono.error(exceptionMap.throwException(
                    Constants.ExceptionName.TOKEN_VALIDATION,
                    Constants.ExceptionMessage.TOKEN_VALIDATION
            ));
        }
    }

    private Mono<String> generateSessionAndSaveToCache(OIDCProviderToken oidcToken, MilAuthAccessToken milToken) {
        String fiscalCode = oidcService.extractFiscalCodeFromIdToken(oidcToken.getIdToken());
        String sessionId = Utils.generateUUID(podInfo.getPodId());
        log.info("[WEBVIEW-SERVICE][TOKEN] Generated sessionId: {}", sessionId);
        try {
            return cacheService.saveToCache(sessionId, objectMapper.writeValueAsString(milToken))
                    .flatMap(result -> {
                        if (Boolean.TRUE.equals(result)) {
                            log.info("[WEBVIEW-SERVICE][TOKEN] Session saved to cache: {}", sessionId);
                            return cacheService.saveToCache(milToken.getAccessToken(), fiscalCode)
                                    .flatMap(accessTokenResult -> {
                                        if (Boolean.TRUE.equals(accessTokenResult)) {
                                            String tokenRedirectUrl = redirectInfo.getTokenRedirect().replace("<session-id>", sessionId);
                                            log.info("[WEBVIEW-SERVICE][TOKEN] Token redirect URL generated: {}", tokenRedirectUrl);
                                            return Mono.just(tokenRedirectUrl);
                                        } else {
                                            log.error("[WEBVIEW-SERVICE][TOKEN] Failed to save user to cache");
                                            return Mono.error(exceptionMap.throwException(
                                                    Constants.ExceptionName.USER_SAVE_FAIL,
                                                    Constants.ExceptionMessage.USER_SAVE_FAIL
                                            ));
                                        }
                                    });
                        } else {
                            log.error("[WEBVIEW-SERVICE][TOKEN] Failed to save token to cache");
                            return Mono.error(exceptionMap.throwException(
                                    Constants.ExceptionName.TOKEN_SAVE_FAIL,
                                    Constants.ExceptionMessage.TOKEN_SAVE_FAIL
                            ));
                        }
                    });
        } catch (JsonProcessingException e) {
            log.error("[WEBVIEW-SERVICE][TOKEN] Error serializing token", e);
            return Mono.error(exceptionMap.throwException(
                    Constants.ExceptionName.TOKEN_DESERIALIZATION,
                    Constants.ExceptionMessage.TOKEN_DESERIALIZATION
            ));
        }
    }
}