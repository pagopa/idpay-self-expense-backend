package it.gov.pagopa.self.expense.controller;


import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import it.gov.pagopa.self.expense.service.WebviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class WebviewControllerImpl implements  WebviewController{

    private final WebviewService webviewService;

    public WebviewControllerImpl(WebviewService webviewService) {
        this.webviewService = webviewService;
    }

    @Override
    public Mono<ResponseEntity<Void>> login() {
        return webviewService.login()
                .map(deepLink ->ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", deepLink)
                        .build()
                );
    }

    @Override
    public Mono<ResponseEntity<Void>> token(String authCode, String state) {
        return webviewService.token(authCode, state)
                .map(deepLink ->ResponseEntity.status(HttpStatus.FOUND)
                        .header("Location", deepLink)
                        .build()
                );
    }

    @Override
    @CrossOrigin(origins = "https://welfare.uat.cstar.pagopa.it/")
    public Mono<ResponseEntity<MilAuthAccessToken>> session(String sessionId) {
        return webviewService.session(sessionId)
                .map(ResponseEntity::ok);
    }
    @Override
    public Mono<ResponseEntity<MilAuthAccessToken>> mock(String sessionId) {
        return webviewService.mock(sessionId)
                .map(ResponseEntity::ok);
    }
}
