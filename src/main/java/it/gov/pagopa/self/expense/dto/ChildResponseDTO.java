package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.Child;
import lombok.Data;

import java.util.List;

@Data
public class ChildResponseDTO {

    private List<Child> childList;
}
