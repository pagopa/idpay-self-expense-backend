package it.gov.pagopa.self.expense.service.commands;


import it.gov.pagopa.self.expense.configuration.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface SelfExpenseErrorNotifierService {
    void notifySelfExpenseCommands(Message<String> message, String description, boolean retryable, Throwable exception);
    void notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}
