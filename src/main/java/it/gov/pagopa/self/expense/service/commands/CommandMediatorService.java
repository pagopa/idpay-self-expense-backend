package it.gov.pagopa.self.expense.service.commands;

import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

public interface CommandMediatorService {
    void execute(Flux<Message<String>> initiativeDTOFlux);
}
