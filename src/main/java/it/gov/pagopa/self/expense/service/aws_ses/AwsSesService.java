package it.gov.pagopa.self.expense.service.aws_ses;

import reactor.core.publisher.Mono;

public interface AwsSesService {

    Mono<String> sendEmail(String to, String subject, String body);

}
