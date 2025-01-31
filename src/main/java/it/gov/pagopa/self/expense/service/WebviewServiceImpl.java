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
        return cacheService.saveToCache(state, state)
                .flatMap(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        return Mono.just(redirectInfo.getLoginRedirect()
                                .replace("<state>", state)
                                .replace("<client_id>", redirectInfo.getClientId())
                                .replace("<redirect_uri>", redirectInfo.getRedirectUri()));
                    } else {
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.LOGIN_FAIL,
                                Constants.ExceptionMessage.LOGIN_FAIL
                            ));
                    }
                });
    }


    @Override
    public Mono<MilAuthAccessToken> session(String sessionId) {
        return cacheService.getFromCache(sessionId)
                .switchIfEmpty(Mono.error(exceptionMap.throwException(
                        Constants.ExceptionName.SESSION_NOT_FOUND,
                        Constants.ExceptionMessage.SESSION_NOT_FOUND
                )))
                .flatMap(token -> {
                    try {
                        MilAuthAccessToken accessToken = objectMapper.readValue(token, MilAuthAccessToken.class);
                        return Mono.just(accessToken);
                    } catch (Exception e) {
                        log.error("Error deserializing token for session: {}", sessionId, e);
                        return Mono.error(exceptionMap.throwException(
                                Constants.ExceptionName.SESSION_FAIL,
                                Constants.ExceptionMessage.SESSION_FAIL
                        ));
                    }
                });
    }

    @Override
    public Mono<String> token(String authCode, String state) {
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
        if (oidcService.validateTokens(oidcToken)) {
            return milAuthConnector.token()
                    .flatMap(milToken -> generateSessionAndSaveToCache(oidcToken, milToken));
        } else {
            return Mono.error(exceptionMap.throwException(
                    Constants.ExceptionName.TOKEN_VALIDATION,
                    Constants.ExceptionMessage.TOKEN_VALIDATION
            ));
        }
    }

    private Mono<String> generateSessionAndSaveToCache(OIDCProviderToken oidcToken, MilAuthAccessToken milToken) {
        String fiscalCode = oidcService.extractFiscalCodeFromIdToken(oidcToken.getIdToken());
        String sessionId = Utils.generateUUID(podInfo.getPodId());
        try {
            return cacheService.saveToCache(sessionId, objectMapper.writeValueAsString(milToken))
                    .flatMap(result -> {
                        if (Boolean.TRUE.equals(result)) {
                            return cacheService.saveToCache(milToken.getAccessToken(), fiscalCode)
                                    .flatMap(accessTokenResult -> {
                                        if (Boolean.TRUE.equals(accessTokenResult)) {
                                            return Mono.just(redirectInfo.getTokenRedirect().replace("<session-id>", sessionId));
                                        } else {
                                            return Mono.error(exceptionMap.throwException(
                                                    Constants.ExceptionName.USER_SAVE_FAIL,
                                                    Constants.ExceptionMessage.USER_SAVE_FAIL
                                            ));
                                        }
                                    });
                        } else {
                            return Mono.error(exceptionMap.throwException(
                                    Constants.ExceptionName.TOKEN_SAVE_FAIL,
                                    Constants.ExceptionMessage.TOKEN_SAVE_FAIL
                            ));
                        }
                    });
        } catch (JsonProcessingException e) {
            return Mono.error(exceptionMap.throwException(
                    Constants.ExceptionName.TOKEN_DESERIALIZATION,
                    Constants.ExceptionMessage.TOKEN_DESERIALIZATION
            ));
        }
    }

}
