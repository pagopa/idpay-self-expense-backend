package it.gov.pagopa.self.expense.event.producer;


import it.gov.pagopa.common.utils.CommonUtilities;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.RtdQueueMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;


@Component
@Slf4j
public class RtdProducer {

  private final String binder;
  private final StreamBridge streamBridge;


  public RtdProducer(StreamBridge streamBridge,
                     @Value("${spring.cloud.stream.bindings.trxProducer-out-0.binder}")String binder) {
    this.streamBridge = streamBridge;
    this.binder = binder;
  }

  private Message<RtdQueueMessage> buildMessage(ExpenseDataDTO expenseData) {
    Long amountCents = CommonUtilities.euroToCents(expenseData.getAmount());

    RtdQueueMessage rtdQueueMessage = RtdQueueMessage.builder()
            .idTrxAcquirer(UUID.randomUUID().toString().concat("_ACQUIRER_TRX").concat(String.valueOf(OffsetDateTime.now().toEpochSecond())))
            .acquirerCode("ACQUIRER_CODE")
            .acquirerId("ACQUIRER_ID")
            .trxDate(OffsetDateTime.now())
            .hpan("IDPAY_".concat(expenseData.getFiscalCode()))
            .operationType("00")
            .correlationId(UUID.randomUUID().toString().concat("_RTD_").concat(String.valueOf(OffsetDateTime.now().toEpochSecond())))
            .amount(BigDecimal.valueOf(amountCents))
            .amountCurrency("EUR")
            .fiscalCode(expenseData.getFiscalCode())
            .businessName(expenseData.getCompanyName())
            .build();

    return MessageBuilder
            .withPayload(rtdQueueMessage)
            .build();
  }
  public Mono<Void> scheduleMessage(ExpenseDataDTO expenseDataDTO) {
    return Mono
            .fromCallable(() -> streamBridge.send("trxProducer-out-0", binder, buildMessage(expenseDataDTO)))
            .then();
  }


}



