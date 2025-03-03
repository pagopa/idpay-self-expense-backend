package it.gov.pagopa.self.expense.utils;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
public final class Utils {
    public static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private Utils(){}

    public static String generateUUID(String podId) {
        long timestamp = Instant.now().toEpochMilli();
        String randomUUID = UUID.randomUUID().toString();
        return randomUUID + "-" + podId + "-" +timestamp;
    }

    public static Long euroToCents(BigDecimal euro){
        return euro == null? null : euro.multiply(ONE_HUNDRED).longValue();
    }
}
