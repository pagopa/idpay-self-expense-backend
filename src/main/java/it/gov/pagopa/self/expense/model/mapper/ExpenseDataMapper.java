package it.gov.pagopa.self.expense.model.mapper;

import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.ExpenseData;

public class ExpenseDataMapper {

    public static ExpenseData map(ExpenseDataDTO dto){
        return ExpenseData.builder()
                .name(dto.getName())
                .surname(dto.getSurname())
                .amount(dto.getAmount())
                .expenseDate(dto.getExpenseDate())
                .companyName(dto.getCompanyName())
                .entityId(dto.getEntityId())
                //TODO da confermare se deve essere hashato
                .userId(dto.getFiscalCode())
                .initiativeId(dto.getInitiativeId())
                .file(dto.getFile())
                .build();
    }
}
