package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.AnprInfo;
import it.gov.pagopa.self.expense.model.Child;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.model.FileData;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    @MockBean
    private ExpenseDataRepository expenseDataRepository;

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



    @Test
    void testSaveExpenseData_Failure() {

        ExpenseDataDTO dto = buildExpenseDataDTO();

        Mockito.when(expenseDataRepository.save(ExpenseDataMapper.map(dto)))
                .thenThrow(new IllegalArgumentException("Error on db"));

        Mockito.when(expenseDataRepository.save(Mockito.any(ExpenseData.class)))
                .thenReturn(Mono.error(new RuntimeException("DB error")));

        Mono<Void> result = selfExpenseService.saveExpenseData(dto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                        throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();
    }

    @Test
    void testSaveExpenseData_Success() {
        ExpenseDataDTO expenseDataDTO = buildExpenseDataDTO();
        ExpenseData expenseData = ExpenseDataMapper.map(expenseDataDTO);

        Mockito.when(expenseDataRepository.save(ExpenseDataMapper.map(expenseDataDTO))).thenReturn(Mono.just(expenseData));

        Mono<Void> result = selfExpenseService.saveExpenseData(expenseDataDTO);

        StepVerifier.create(result)
                .verifyComplete();
    }

    private static ExpenseDataDTO buildExpenseDataDTO() {
        FileData fileData = new FileData();
        fileData.setData("fileData");
        fileData.setFilename("file.pdf");
        fileData.setContentType("file/pdf");
        List<FileData> fileList = new ArrayList<>();
        fileList.add(fileData);
        return ExpenseDataDTO.builder()
                .name("nome")
                .surname("surname")
                .amount(10.20)
                .expenseDate(LocalDateTime.now())
                .companyName("company")
                .entityId("entityId")
                .fiscalCode("ABCQWE89T08H224W")
                .initiativeId("initiative")
                .fileList(fileList)
                .build();

    }




}
