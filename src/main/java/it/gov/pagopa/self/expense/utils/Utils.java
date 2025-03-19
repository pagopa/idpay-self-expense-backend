package it.gov.pagopa.self.expense.utils;

import it.gov.pagopa.self.expense.dto.ReportExcelDTO;
import it.gov.pagopa.self.expense.model.SelfDeclarationTextValues;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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

    public static String formatDeclaration(List<SelfDeclarationTextValues> declarationList){
        StringBuilder formattedString = new StringBuilder();
        if (declarationList!=null)
            for (SelfDeclarationTextValues declaration : declarationList) {
                String description = declaration.getDescription();
                String value = declaration.getValue();
                formattedString.append(description)
                        .append(" ").append(value)
                        .append("\n");
            }
        return formattedString.toString().trim();

    }

    public static List<List<String>> generateRowValuesForReport(List<ReportExcelDTO> dtoList){

        List<List<String>> result = new ArrayList<>();
        List<List<String>> resultForUser = null;

        for(ReportExcelDTO reportExcelDTO : dtoList){
            resultForUser = reportExcelDTO.mapToRowValues();
            result.addAll(resultForUser);
        }
        return result;
    }
}
