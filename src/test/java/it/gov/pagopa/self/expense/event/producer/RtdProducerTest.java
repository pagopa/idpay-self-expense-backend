package it.gov.pagopa.self.expense.event.producer;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.Optional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RtdProducer.class,
})
@TestPropertySource(properties = {
        "spring.cloud.stream.bindings.trxProducer-out-0.binder=binder",
})
class RtdProducerTest {

    @MockBean
    private StreamBridge streamBridge;

    @Autowired
    private RtdProducer rtdProducer;

    @Test
     void testScheduleMessage() {
        ExpenseDataDTO expenseData = ExpenseDataDTO.builder()
                .amount(new BigDecimal("100.0"))
                .fiscalCode("testFiscalCode")
                .build();


        String userId = "testUserId";

        Mockito.when(streamBridge.send(Mockito.eq("trxProducer-out-0"), Optional.ofNullable(Mockito.eq("binder")), Mockito.any())).thenReturn(true);

        Mono<Void> result = rtdProducer.scheduleMessage(expenseData);

        StepVerifier.create(result)
                .expectComplete()
                .verify();

    }
}