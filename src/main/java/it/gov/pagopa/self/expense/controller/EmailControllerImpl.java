package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.MessageDTO;
import it.gov.pagopa.self.expense.service.aws_ses.AwsSesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
public class EmailControllerImpl implements  EmailController{


    private final AwsSesService awsSesService;



    public EmailControllerImpl(AwsSesService awsSesService) {
        this.awsSesService = awsSesService;
    }


    @Override
    public Mono<ResponseEntity<String>> send(@RequestBody MessageDTO dto) {
      return awsSesService.sendEmail(dto.getEmailAddress(),dto.getSubject(),dto.getPlainText());
    }

}