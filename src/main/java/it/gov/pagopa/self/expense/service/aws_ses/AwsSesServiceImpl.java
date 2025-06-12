package it.gov.pagopa.self.expense.service.aws_ses;

import it.gov.pagopa.self.expense.controller.aws_ses.AwsSesConnector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class AwsSesServiceImpl implements AwsSesService {

    private final AwsSesConnector awsSesConnector;

    @Autowired
    public AwsSesServiceImpl(AwsSesConnector awsSesConnector) {
        this.awsSesConnector = awsSesConnector;
    }

    @Override
    public Mono<String> sendEmail(String to, String subject, String body) {
        log.trace("sendEmail start");
        log.debug("sendEmail , to = {}, subject = {}, body = {}", to, subject, body);
        String result = null;
        try {
            result = awsSesConnector.sendEmail(to, subject, body);
        }catch (Exception e){
            log.error(e.getMessage());
            result = "sendEmail error to = "+to+", subject = "+subject;
        }
        log.debug("sendEmail result = {}", result);
        log.trace("sendEmail end");
        return Mono.just(result);
    }
}