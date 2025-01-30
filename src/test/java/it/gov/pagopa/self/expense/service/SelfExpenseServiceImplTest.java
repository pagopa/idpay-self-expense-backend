package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.model.AnprInfo;
import it.gov.pagopa.self.expense.model.Child;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest(classes = {SelfExpenseServiceImpl.class})
class SelfExpenseServiceImplTest {

    @Autowired
    private SelfExpenseServiceImpl selfExpenseService;

    @MockBean
    private AnprInfoRepository anprInfoRepository;

    @Test
    void getChildForUserId() {

        String userId = "userId_1";
        String initiativeId = "initiative_1";


        //caso anpr_info presente
        AnprInfo result_ok = new AnprInfo();
        result_ok.setUserId(userId);
        result_ok.setInitiativeId(initiativeId);
        result_ok.setFamilyId("family_1");
        Child child1 = new Child("child1", "nome1", "cognome1");
        Child child2 = new Child("child2", "nome2", "cognome2");
        List<Child> childList = new ArrayList<>();
        childList.add(child1);childList.add(child2);
        result_ok.setChildList(childList);

        ChildResponseDTO childResponseDTOExpected = new ChildResponseDTO();
        childResponseDTOExpected.setChildList(result_ok.getChildList());

        Mockito.when(anprInfoRepository.findByUserIdAndInitiativeId(userId, initiativeId)).thenReturn(Mono.just(result_ok));

        StepVerifier.create(selfExpenseService.getChildForUserId(userId, initiativeId))
                        .expectNext(childResponseDTOExpected)
                        .verifyComplete();

    }
}