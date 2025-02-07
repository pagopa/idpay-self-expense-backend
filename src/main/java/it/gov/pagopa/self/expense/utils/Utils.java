package it.gov.pagopa.self.expense.utils;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.UUID;

@Slf4j
public final class Utils {
    private Utils(){}

    public static String generateUUID(String podId) {
        long timestamp = Instant.now().toEpochMilli();
        String randomUUID = UUID.randomUUID().toString();
        return randomUUID + "-" + podId + "-" +timestamp;
    }

}
