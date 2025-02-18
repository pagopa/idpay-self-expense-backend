package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
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
    private static final String FISCAL_CODE = "fiscalCode";
    private static final String MIL_AUTH_TOKEN = "milAuthToken";
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

    @MockBean
    private UserFiscalCodeService userFiscalCodeService;

    @MockBean
    private RtdProducer rtdProducer;

    @MockBean
    private CacheService cacheService;

    @Autowired
    private ExceptionMap exceptionMap;

    @Test
    void shouldReturnChildList_WhenUserExists() {
        AnprInfo anprInfo = buildAnprInfo();
        ChildResponseDTO childResponseDTO = buildChildResponseDTO(anprInfo);

        Mockito.when(cacheService.getFromCache(MIL_AUTH_TOKEN)).thenReturn(Mono.just(FISCAL_CODE));

        Mockito.when(userFiscalCodeService.getUserId(FISCAL_CODE)).thenReturn(Mono.just(USER_ID));

        Mockito.when(anprInfoRepository.findByUserId(USER_ID)).thenReturn(Mono.just(anprInfo));

        StepVerifier.create(selfExpenseService.getChildForUserId(MIL_AUTH_TOKEN))
                .expectNext(childResponseDTO)
                .verifyComplete();
    }

    @Test
    void shouldReturnError_WhenUserNotFound() {


        Mockito.when(cacheService.getFromCache(MIL_AUTH_TOKEN)).thenReturn(Mono.just(FISCAL_CODE));

        Mockito.when(userFiscalCodeService.getUserId(FISCAL_CODE)).thenReturn(Mono.just(USER_ID));

        Mockito.when(anprInfoRepository.findByUserId(USER_ID))
                .thenReturn(Mono.empty());

        StepVerifier.create(selfExpenseService.getChildForUserId(MIL_AUTH_TOKEN))
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
        child.setUserId(USER_ID);
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
        childResponseDTO.setUserId(anprInfo.getUserId());
        return childResponseDTO;
    }



    @Test
    void testSaveExpenseData_Failure() {

        ExpenseDataDTO dto = buildExpenseDataDTO();

        Mockito.when(expenseDataRepository.save(Mockito.any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        Mockito.when(userFiscalCodeService.getUserId(dto.getFiscalCode()))
                .thenReturn(Mono.just("userId"));


        Mono<Void> result = selfExpenseService.saveExpenseData(dto);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();

        Mockito.verifyNoInteractions(rtdProducer);

    }

    @Test
    void testSaveExpenseData_Success() {
        ExpenseDataDTO expenseDataDTO = buildExpenseDataDTO();
        ExpenseData expenseData = ExpenseDataMapper.map(expenseDataDTO);

        Mockito.when(expenseDataRepository.save(ExpenseDataMapper.map(expenseDataDTO))).thenReturn(Mono.just(expenseData));

        Mockito.when(userFiscalCodeService.getUserId(expenseDataDTO.getFiscalCode()))
                .thenReturn(Mono.just("userId"));

        Mockito.when(rtdProducer.scheduleMessage(expenseDataDTO,"userId")).thenReturn(Mono.empty());
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
                .description("initiative")
                .fileList(fileList)
                .build();

    }




}
