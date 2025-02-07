package it.gov.pagopa.self.expense.service.commands;

import ch.qos.logback.classic.LoggerContext;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.utils.MemoryAppender;
import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.self.expense.dto.commands.QueueCommandOperationDTO;
import it.gov.pagopa.self.expense.utils.CommandConstants;
import it.gov.pagopa.self.expense.utils.faker.QueueCommandOperationDTOFaker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
class CommandMediatorServiceImplTest {

    @Mock
    private SelfExpenseErrorNotifierService selfExpenseErrorNotifierService;
    @Mock
    private DeleteInitiativeService deleteInitiativeServiceMock;
    @Mock
    private Message<String> messageMock;
    private CommandMediatorServiceImpl commandMediatorService;
    private MemoryAppender memoryAppender;

    @BeforeEach
    void setUp() {
        commandMediatorService =
                new CommandMediatorServiceImpl(
                        "Application Name",
                        100L,
                        "PT1S",
                        selfExpenseErrorNotifierService,
                        deleteInitiativeServiceMock,
                        TestUtils.objectMapper);

        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger("it.gov.pagopa.self.expense.service.commands.CommandMediatorServiceImpl");
        memoryAppender = new MemoryAppender();
        memoryAppender.setContext((LoggerContext) LoggerFactory.getILoggerFactory());
        logger.setLevel(ch.qos.logback.classic.Level.INFO);
        logger.addAppender(memoryAppender);
        memoryAppender.start();
    }

    @Test
    void getCommitDelay() {
        //given
        Duration expected = Duration.ofMillis(100L);
        //when
        Duration commitDelay = commandMediatorService.getCommitDelay();
        //then
        Assertions.assertEquals(expected,commitDelay);
    }

    @Test
    void givenMessagesWhenAfterCommitsThenSuccessfully() {
        //given
        Flux<List<String>> afterCommits2Subscribe = Flux.just(List.of("INITIATIVE1","INITIATIVE2","INITIATIVE3"));
          // when
        commandMediatorService.subscribeAfterCommits(afterCommits2Subscribe);

        //then
        Assertions.assertEquals(
                "[SELF_EXPENSE_COMMANDS] Processed offsets committed successfully",
                memoryAppender.getLoggedEvents().get(0).getFormattedMessage()
        );
    }
    @Test
    void givenErrorWhenNotifyErrorThenCallNotifierService() {
        Throwable error = new RuntimeException("Test error");
        commandMediatorService.notifyError(messageMock, error);
        Mockito.verify(selfExpenseErrorNotifierService).notifySelfExpenseCommands(
                messageMock,
                "[SELF_EXPENSE_COMMANDS] An error occurred evaluating commands",
                true,
                error
        );
    }
    @Test
    void givenDeserializationErrorWhenOnDeserializationErrorThenCallNotifierService() {
        Throwable error = new RuntimeException("Test error");
        commandMediatorService.onDeserializationError(messageMock).accept(error);
        Mockito.verify(selfExpenseErrorNotifierService).notifySelfExpenseCommands(
                messageMock,
                "[SELF_EXPENSE_COMMANDS] Unexpected JSON",
                false,
                error
        );
    }
    @Test
    void getObjectReader() {
        ObjectReader objectReader = commandMediatorService.getObjectReader();
        Assertions.assertNotNull(objectReader);
    }

    @Test
    void givenDeleteInitiatveOperationTypeWhenCallExecuteThenReturnString() {
        //given
        QueueCommandOperationDTO payload = QueueCommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType(CommandConstants.OPERATION_TYPE_DELETE_INITIATIVE)
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();

        Mockito.when(deleteInitiativeServiceMock.execute(payload.getEntityId())).thenReturn(Mono.just(anyString()));

        //when
        String result = commandMediatorService.execute(payload, message, ctx).block();

        //then
        Assertions.assertNotNull(result);
        Mockito.verify(deleteInitiativeServiceMock).execute(anyString());
    }

    @Test
    void givenOperationTypeDifferentWhenCallExecuteThenReturnMonoEmpty(){
        //given
        QueueCommandOperationDTO payload = QueueCommandOperationDTO.builder()
                .entityId("DUMMY_INITITATIVEID")
                .operationTime(LocalDateTime.now())
                .operationType("OTHER_OPERATION_TYPE")
                .build();

        Message<String> message = MessageBuilder.withPayload("INITIATIVE").setHeader("HEADER","DUMMY_HEADER").build();
        Map<String, Object> ctx = new HashMap<>();
        //when
        Mono<String> result= commandMediatorService.execute(payload, message, ctx);

        //then
        assertEquals(result,Mono.empty());
        Mockito.verify(deleteInitiativeServiceMock,Mockito.never()).execute(anyString());
    }
    @Test
    void getFlowName() {
        //given
        String expected = "SELF_EXPENSE_COMMANDS";
        //when
        String result = commandMediatorService.getFlowName();
        //then
        Assertions.assertEquals(expected,result);
    }

    @ParameterizedTest
    @ValueSource(longs = {800,1000,1010})
    void testSuccessful(long commitDelay){
        // Given
        int N = 10;
        List<QueueCommandOperationDTO> initiatives = IntStream.range(0, N).mapToObj(QueueCommandOperationDTOFaker::mockInstance).collect(Collectors.toList());
        Flux<Message<String>> inputFlux = Flux.fromIterable(initiatives)
                .map(TestUtils::jsonSerializer)
                .map(payload -> MessageBuilder
                        .withPayload(payload)
                        .setHeader(KafkaHeaders.RECEIVED_PARTITION, 0)
                        .setHeader(KafkaHeaders.OFFSET, 0L)
                )
                .map(MessageBuilder::build);

        CommandMediatorService service = new CommandMediatorServiceImpl("appName", commitDelay,"PT1S", selfExpenseErrorNotifierService, deleteInitiativeServiceMock, TestUtils.objectMapper);

        // when
        Mockito.when(deleteInitiativeServiceMock.execute("entityId")).thenReturn(Mono.just("true"));
        service.execute(inputFlux);

        // then
        Mockito.verify(deleteInitiativeServiceMock, Mockito.times(N)).execute(Mockito.any());

    }
}