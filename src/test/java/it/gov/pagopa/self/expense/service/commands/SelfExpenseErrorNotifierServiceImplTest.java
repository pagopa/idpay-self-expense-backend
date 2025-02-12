package it.gov.pagopa.self.expense.service.commands;


import it.gov.pagopa.common.kafka.service.ErrorNotifierService;
import it.gov.pagopa.self.expense.configuration.KafkaConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class SelfExpenseErrorNotifierServiceImplTest {
    private static final String BINDER_KAFKA_TYPE = "kafka";
    private static final String BINDER_BROKER = "broker";
    private static final String DUMMY_MESSAGE = "DUMMY MESSAGE";
    private static final Message<String> dummyMessage = MessageBuilder.withPayload(DUMMY_MESSAGE).build();
    private static Map<String, KafkaConfiguration.KafkaInfoDTO> bindingsMap;

    @Mock
    private ErrorNotifierService errorNotifierServiceMock;
    @Mock
    private KafkaConfiguration kafkaConfigurationMock;

    private SelfExpenseErrorNotifierServiceImpl admissibilityErrorNotifierService;

    @BeforeEach
    void setUp() {
        admissibilityErrorNotifierService = new SelfExpenseErrorNotifierServiceImpl(
                errorNotifierServiceMock,
                kafkaConfigurationMock
        );
        bindingsMap = Map.of(
                "consumerCommands-in-0", KafkaConfiguration.KafkaInfoDTO.builder()
                        .type(BINDER_KAFKA_TYPE)
                        .brokers(BINDER_BROKER)
                        .destination( "commands-topic")
                        .group("commands-group")
                        .build()
        );

    }

    @Test
    void notifyAdmissibilityCommands() {
        Mockito.when(kafkaConfigurationMock.getStream()).thenReturn(Mockito.mock(KafkaConfiguration.Stream.class));
        Mockito.when(kafkaConfigurationMock.getStream().getBindings()).thenReturn(bindingsMap);

        errorNotifyMock(bindingsMap.get("consumerCommands-in-0"),true,true);

        admissibilityErrorNotifierService.notifySelfExpenseCommands(dummyMessage, DUMMY_MESSAGE, true, new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    @Test
    void testNotify() {
        errorNotifyMock(bindingsMap.get("consumerCommands-in-0"),true,true);
        admissibilityErrorNotifierService.notify(bindingsMap.get("consumerCommands-in-0"),dummyMessage,DUMMY_MESSAGE,true,true,new Throwable(DUMMY_MESSAGE));
        Mockito.verifyNoMoreInteractions(errorNotifierServiceMock,kafkaConfigurationMock);
    }

    private void errorNotifyMock(KafkaConfiguration.BaseKafkaInfoDTO basekafkaInfoDTO, boolean retryable, boolean resendApplication ) {
        Mockito.when(errorNotifierServiceMock.notify(eq(basekafkaInfoDTO), eq(dummyMessage), eq(DUMMY_MESSAGE), eq(retryable), eq(resendApplication), any()))
                .thenReturn(true);
    }
}