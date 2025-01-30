package it.gov.pagopa.self.expense.controller;


import it.gov.pagopa.self.expense.model.MilAuthAccessToken;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;


public interface WebviewController {

    @GetMapping("/login")
    Mono<ResponseEntity<Void>> login();

    @GetMapping("/redirect")
    Mono<ResponseEntity<Void>> token(@RequestParam("code") String authCode, @RequestParam("state") String state);

    @GetMapping("/session/{sessionId}")
    Mono<ResponseEntity<MilAuthAccessToken>> session(@PathVariable  String sessionId);

}
