package it.gov.pagopa.self.expense.model.mapper;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.utils.Utils;

public class ExpenseDataMapper {

    public static ExpenseData map(ExpenseDataDTO dto){
        return ExpenseData.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .amountCents(Utils.euroToCents(dto.getAmount()))
                .expenseDate(dto.getExpenseDate())
                .companyName(dto.getCompanyName())
                .entityId(dto.getEntityId())
                .userId(dto.getFiscalCode())
                .description(dto.getDescription())
                .fileList(dto.getFileList())
                .build();
    }
}
