package it.gov.pagopa.self.expense.event.cunsomer;


import it.gov.pagopa.self.expense.service.commands.CommandMediatorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;

@Configuration
@Slf4j
public class CommandConsumerConfig {

    @Bean
    public Consumer<Flux<Message<String>>> consumerCommands(CommandMediatorService commandMediatorService) {
        return commandMediatorService::execute;
    }
}
