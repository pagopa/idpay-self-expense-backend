package it.gov.pagopa.self.expense.controller;


import it.gov.pagopa.self.expense.dto.email.MessageDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RequestMapping("/idpay/email")
public interface EmailController {

    @GetMapping("/send")
    Mono<ResponseEntity<String>> send(@RequestBody MessageDTO dto);


}
