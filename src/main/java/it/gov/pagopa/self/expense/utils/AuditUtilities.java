package it.gov.pagopa.self.expense.utils;

import it.gov.pagopa.common.utils.AuditLogger;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j(topic = "AUDIT")
public class AuditUtilities {

    private static final String CEF = String.format("CEF:0|PagoPa|IDPAY|1.0|7|User interaction|2| event=SelfExpense dstip=%s", AuditLogger.SRCIP);
    private static final String CEF_USER_PATTERN_DELETE = CEF + " msg={} cs1Label=userId cs1={} cs2Label=initiativeId cs2={}";

    public void logDeletedExpenseData(String userId, String initiativeId) {
        AuditLogger.logAuditString(
                CEF_USER_PATTERN_DELETE,
                "Expense data deleted.", userId, initiativeId
        );
    }
}
