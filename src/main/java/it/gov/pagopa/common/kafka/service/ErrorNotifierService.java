package it.gov.pagopa.common.kafka.service;

import it.gov.pagopa.self.expense.configuration.KafkaConfiguration;
import org.springframework.messaging.Message;

public interface ErrorNotifierService {
    boolean notify(KafkaConfiguration.BaseKafkaInfoDTO baseKafkaInfoDTO, Message<?> message, String description, boolean retryable, boolean resendApplication, Throwable exception);
}