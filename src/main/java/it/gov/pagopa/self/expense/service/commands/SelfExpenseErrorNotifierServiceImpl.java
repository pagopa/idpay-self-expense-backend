package it.gov.pagopa.self.expense.service.commands;


import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.self.expense.configuration.KafkaConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SelfExpenseErrorNotifierServiceImpl implements SelfExpenseErrorNotifierService {

    private static final String KAFKA_BINDINGS_SELF_EXPENSE_COMMANDS = "consumerCommands-in-0";

    private final ErrorNotifierService errorNotifierService;
    private final KafkaConfiguration kafkaConfiguration;

    public SelfExpenseErrorNotifierServiceImpl(ErrorNotifierService errorNotifierService,
                                               KafkaConfiguration kafkaConfiguration) {
        this.errorNotifierService = errorNotifierService;
        this.kafkaConfiguration = kafkaConfiguration;
    }

    @Override
    public void notifySelfExpenseCommands(Message<String> message, String description, boolean retryable, Throwable exception) {
        notify(kafkaConfiguration.getStream().getBindings().get(KAFKA_BINDINGS_SELF_EXPENSE_COMMANDS), message, description, retryable, true, exception);
    }

    @Override
    public void notify(KafkaConfiguration.BaseKafkaInfoDTO basekafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception) {
        errorNotifierService.notify(basekafkaInfoDTO, message, description, retryable,resendApplication, exception);
    }

}
