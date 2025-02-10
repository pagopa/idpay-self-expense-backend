package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.Child;
import lombok.Data;

import java.util.List;

@Data
public class ChildResponseDTO {
    private String userId; // fiscalCode del richiedente criptato
    private List<Child> childList;
}
