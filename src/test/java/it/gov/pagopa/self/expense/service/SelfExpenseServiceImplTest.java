package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.model.AnprInfo;
import it.gov.pagopa.self.expense.model.Child;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

@SpringBootTest(classes = { SelfExpenseServiceImpl.class, ExceptionMap.class })
class SelfExpenseServiceImplTest {

    private static final String USER_ID = "userId";
    private static final String INITIATIVE_ID = "initiativeId";
    private static final String FAMILY_ID = "familyId";
    private static final String CHILD_NAME = "nome";
    private static final String CHILD_SURNAME = "cognome";

    @Autowired
    private SelfExpenseServiceImpl selfExpenseService;

    @MockBean
    private AnprInfoRepository anprInfoRepository;

    @Autowired
    private ExceptionMap exceptionMap;

    @Test
    void shouldReturnChildList_WhenUserExists() {
        AnprInfo anprInfo = buildAnprInfo();
        ChildResponseDTO childResponseDTO = buildChildResponseDTO(anprInfo);

        Mockito.when(anprInfoRepository.findByUserIdAndInitiativeId(USER_ID, INITIATIVE_ID))
                .thenReturn(Mono.just(anprInfo));

        StepVerifier.create(selfExpenseService.getChildForUserId(USER_ID, INITIATIVE_ID))
                .expectNext(childResponseDTO)
                .verifyComplete();
    }

    @Test
    void shouldReturnError_WhenUserNotFound() {
        Mockito.when(anprInfoRepository.findByUserIdAndInitiativeId(USER_ID, INITIATIVE_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(selfExpenseService.getChildForUserId(USER_ID, INITIATIVE_ID))
                .expectErrorMatches(throwable ->
                        throwable.getMessage().contains(Constants.ExceptionMessage.ANPR_INFO_NOT_FOUND)
                )
                .verify();
    }


    private AnprInfo buildAnprInfo() {
        Child child = new Child();
        child.setCognome(CHILD_SURNAME);
        child.setUserId(USER_ID);
        child.setNome(CHILD_NAME);

        AnprInfo anprInfo = new AnprInfo();
        anprInfo.setUserId(USER_ID);
        anprInfo.setInitiativeId(INITIATIVE_ID);
        anprInfo.setFamilyId(FAMILY_ID);
        anprInfo.setChildList(List.of(child));

        return anprInfo;
    }

    private ChildResponseDTO buildChildResponseDTO(AnprInfo anprInfo){
        ChildResponseDTO childResponseDTO = new ChildResponseDTO();
        childResponseDTO.setChildList(anprInfo.getChildList());
        return childResponseDTO;
    }
}
