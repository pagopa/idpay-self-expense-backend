package it.gov.pagopa.self.expense.service.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import it.gov.pagopa.common.reactive.kafka.consumer.BaseKafkaConsumer;
import it.gov.pagopa.self.expense.dto.commands.QueueCommandOperationDTO;
import it.gov.pagopa.self.expense.utils.CommandConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
@Service
@Slf4j
public class CommandMediatorServiceImpl extends BaseKafkaConsumer<QueueCommandOperationDTO, String> implements CommandMediatorService{

    private final Duration commitDelay;
    private final Duration beneficiaryRulesBuildDelayMinusCommit;
    private final DeleteInitiativeService deleteInitiativeService;
    private final SelfExpenseErrorNotifierService selfExpenseErrorNotifierService;
    private final ObjectReader objectReader;

    public CommandMediatorServiceImpl(@Value("${spring.application.name}") String applicationName,
                                      @Value("${spring.cloud.stream.kafka.bindings.consumerCommands-in-0.consumer.ackTime}")  long commitMillis,
                                      @Value("${app.beneficiary-rule.build-delay-duration}") String beneficiaryRulesBuildDelay,
                                      DeleteInitiativeService deleteInitiativeService,
                                      SelfExpenseErrorNotifierService selfExpenseErrorNotifierService,
                                      ObjectMapper objectMapper) {
        super(applicationName);
        this.commitDelay = Duration.ofMillis(commitMillis);
        this.deleteInitiativeService = deleteInitiativeService;
        this.selfExpenseErrorNotifierService = selfExpenseErrorNotifierService;
        this.objectReader = objectMapper.readerFor(QueueCommandOperationDTO.class);

        Duration beneficiaryRulesBuildDelayDuration = Duration.parse(beneficiaryRulesBuildDelay).minusMillis(commitMillis);
        Duration defaultDurationDelay = Duration.ofMillis(2L);
        this.beneficiaryRulesBuildDelayMinusCommit = defaultDurationDelay.compareTo(beneficiaryRulesBuildDelayDuration) >= 0 ? defaultDurationDelay : beneficiaryRulesBuildDelayDuration;
    }

    @Override
    protected Duration getCommitDelay() {
        return commitDelay;
    }

    @Override
    protected void subscribeAfterCommits(Flux<List<String>> afterCommits2subscribe) {
        afterCommits2subscribe
                .buffer(beneficiaryRulesBuildDelayMinusCommit)
                .subscribe(r -> log.info("[SELF-EXPENSE-BACKEND] Processed offsets committed successfully"));
    }

    @Override
    protected ObjectReader getObjectReader() {
        return objectReader;
    }

    @Override
    protected Consumer<Throwable> onDeserializationError(Message<String> message) {
        return e -> selfExpenseErrorNotifierService.notifySelfExpenseCommands(message, "[SELF-EXPENSE-BACKENDS] Unexpected JSON", false, e);
    }

    @Override
    protected void notifyError(Message<String> message, Throwable e) {
        selfExpenseErrorNotifierService.notifySelfExpenseCommands(message, "[SELF-EXPENSE-BACKEND] An error occurred evaluating commands", true, e);
    }

    @Override
    protected Mono<String> execute(QueueCommandOperationDTO payload, Message<String> message, Map<String, Object> ctx) {
        if (CommandConstants.OPERATION_TYPE_DELETE_INITIATIVE.equals(payload.getOperationType())) {
            return deleteInitiativeService.execute(payload.getEntityId());
        }
        log.debug("[ADMISSIBILITY_COMMANDS] Invalid operation type {}", payload.getOperationType());
        return Mono.empty();
    }

    @Override
    public String getFlowName() {
        return "SELF-EXPENSE-BACKEND";
    }


}
