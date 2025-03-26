package it.gov.pagopa.self.expense.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Document("anpr_info")
@Data
public class AnprInfo {

    private String familyId;
    private String initiativeId;
    private String userId;
    private List<Child> childList;
    private Integer underAgeNumber;
}
