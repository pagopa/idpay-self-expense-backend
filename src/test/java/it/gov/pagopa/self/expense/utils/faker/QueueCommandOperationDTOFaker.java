package it.gov.pagopa.self.expense.utils.faker;

import it.gov.pagopa.common.utils.TestUtils;
import it.gov.pagopa.self.expense.dto.commands.QueueCommandOperationDTO;

import java.time.LocalDateTime;

public final class QueueCommandOperationDTOFaker {

    private QueueCommandOperationDTOFaker(){}

    /** It will return an example of {@link QueueCommandOperationDTO}. Providing a bias, it will return a pseudo-casual object */
    public static QueueCommandOperationDTO mockInstance(Integer bias){
        return mockInstanceBuilder(bias).build();
    }

    public static QueueCommandOperationDTO.QueueCommandOperationDTOBuilder mockInstanceBuilder(Integer bias){
        QueueCommandOperationDTO.QueueCommandOperationDTOBuilder out = QueueCommandOperationDTO.builder();

       out.operationTime(LocalDateTime.now())
               .operationType("DELETE_INITIATIVE")
               .entityId("entityId-"+bias)
               .build();

        TestUtils.checkNotNullFields(out.build());
        return out;
    }


}
