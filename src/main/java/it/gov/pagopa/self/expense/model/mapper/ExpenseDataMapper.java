package it.gov.pagopa.self.expense.model.mapper;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.ExpenseData;
import org.apache.qpid.proton.amqp.Binary;

import java.io.IOException;

public class ExpenseDataMapper {

    private ExpenseDataMapper(){}

    public static ExpenseData map(ExpenseDataDTO dto){
        return ExpenseData.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .amount(dto.getAmount())
                .expenseDate(dto.getExpenseDate())
                .companyName(dto.getCompanyName())
                .entityId(dto.getEntityId())
                .userId(dto.getFiscalCode())
                .description(dto.getDescription())
                .fileList(dto.getFileList().stream().map(file -> {
                    try {
                        return new Binary(file.getBytes());
                    } catch (IOException e) {
                        throw new RuntimeException("Error processing file: " + file.getOriginalFilename(), e);
                    }
                }).toList())
                .build();
    }
}
