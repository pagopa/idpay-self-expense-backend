package it.gov.pagopa.self.expense.service.aws_ses;

import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;

public interface AwsSesService {

    Mono<ResponseEntity<String>> sendEmail(String to, String subject, String body);

}
