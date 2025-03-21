package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.connector.FileStorageAsyncConnector;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.dto.ReportExcelDTO;
import it.gov.pagopa.self.expense.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
import it.gov.pagopa.self.expense.model.*;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.self.expense.repository.SelfDeclarationTextRepository;
import it.gov.pagopa.self.expense.utils.MockFilePart;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;

@SpringBootTest(classes = { SelfExpenseServiceImpl.class, ExceptionMap.class })
class SelfExpenseServiceImplTest {

    private static final String USER_ID = "userId";
    private static final String FISCAL_CODE = "fiscalCode";
    private static final String MIL_AUTH_TOKEN = "milAuthToken";
    private static final String INITIATIVE_ID = "initiativeId";
    private static final String FAMILY_ID = "familyId";
    private static final String CHILD_NAME = "nome";
    private static final String CHILD_SURNAME = "cognome";
    private static final String CHILD_NAME_2 = "nome2";
    private static final String CHILD_SURNAME_2 = "cognome2";

    @Autowired
    private SelfExpenseServiceImpl selfExpenseService;

    @MockBean
    private AnprInfoRepository anprInfoRepository;

    @MockBean
    private ExpenseDataRepository expenseDataRepository;

    @MockBean
    private UserFiscalCodeService userFiscalCodeService;

    @MockBean
    private OnboardingFamiliesRepository onboardingFamiliesRepository;

    @MockBean
    private RtdProducer rtdProducer;

    @MockBean
    private CacheService cacheService;
    @MockBean
    private FileStorageAsyncConnector fileStorageAsyncConnector;

    @MockBean
    private SelfDeclarationTextRepository selfDeclarationTextRepository;

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
        anprInfo.setUnderAgeNumber(2);

        return anprInfo;
    }

    private ChildResponseDTO buildChildResponseDTO(AnprInfo anprInfo){
        ChildResponseDTO childResponseDTO = new ChildResponseDTO();
        childResponseDTO.setChildList(anprInfo.getChildList());
        childResponseDTO.setUserId(anprInfo.getUserId());
        return childResponseDTO;
    }

    @Test
    void testSaveExpenseData_Success() {
        List<FilePart> files = MockFilePart.generateMockFileParts();
        ExpenseDataDTO expenseDataDTO = buildExpenseDataDTO();
        ExpenseData expenseData = ExpenseDataMapper.map(expenseDataDTO,files);

        Mockito.when(userFiscalCodeService.getUserId(expenseDataDTO.getFiscalCode()))
                .thenReturn(Mono.just("userId"));

        Mockito.when(expenseDataRepository.save(ExpenseDataMapper.map(expenseDataDTO,files))).thenReturn(Mono.just(expenseData));

        Mockito.when(rtdProducer.scheduleMessage(expenseDataDTO)).thenReturn(Mono.empty());

        Mockito.when(fileStorageAsyncConnector.uploadFile(any(), anyString(), anyString())).thenReturn(Mono.just(true));

        Mono<Void> result = selfExpenseService.saveExpenseData(files, expenseDataDTO);

        StepVerifier.create(result)
                .verifyComplete();

        Mockito.verify(fileStorageAsyncConnector, Mockito.times(0)).delete(any());
    }

    @Test
    void testSaveExpenseData_DB_Failure() {
        ExpenseDataDTO dto = buildExpenseDataDTO();

        Mockito.when(expenseDataRepository.save(any())).thenReturn(Mono.error(new RuntimeException("DB error")));

        Mockito.when(userFiscalCodeService.getUserId(dto.getFiscalCode()))
                .thenReturn(Mono.just("userId"));

        List<FilePart> files = MockFilePart.generateMockFileParts();

        Mono<Void> result = selfExpenseService.saveExpenseData(files, dto);

        Mockito.when(fileStorageAsyncConnector.uploadFile(any(), anyString(), anyString())).thenReturn(Mono.just(true));

        Mockito.when(fileStorageAsyncConnector.delete(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();

        Mockito.verify(fileStorageAsyncConnector, Mockito.times(1)).delete(any());
        Mockito.verifyNoInteractions(rtdProducer);
    }

    @Test
    void testSaveExpenseData_FileValidation_Failure_EmptyFile() {
        ExpenseDataDTO dto = buildExpenseDataDTO();

        List<FilePart> files = MockFilePart.generateMockEmptyFileParts();

        Mono<Void> result = selfExpenseService.saveExpenseData(files, dto);

        Mockito.when(fileStorageAsyncConnector.delete(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();

        Mockito.verify(fileStorageAsyncConnector, Mockito.times(1)).delete(any());
        Mockito.verifyNoInteractions(rtdProducer);
    }

    @Test
    void testSaveExpenseData_FileValidation_Failure_WrongFileType() {
        ExpenseDataDTO dto = buildExpenseDataDTO();

        List<FilePart> files = MockFilePart.generateMockWrongTypeFileParts();

        Mono<Void> result = selfExpenseService.saveExpenseData(files, dto);

        Mockito.when(fileStorageAsyncConnector.delete(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();

        Mockito.verifyNoInteractions(rtdProducer);
    }

    @Test
    void testSaveExpenseData_FileSave_Failure() {
        ExpenseDataDTO dto = buildExpenseDataDTO();

        List<FilePart> files = MockFilePart.generateMockFileParts();

        Mono<Void> result = selfExpenseService.saveExpenseData(files, dto);

        Mockito.when(fileStorageAsyncConnector.uploadFile(any(), anyString(), anyString())).thenReturn(Mono.just(false));

        Mockito.when(fileStorageAsyncConnector.delete(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable.getMessage().equals(Constants.ExceptionMessage.EXPENSE_DATA_ERROR_ON_SAVE_DB))
                .verify();

        Mockito.verify(fileStorageAsyncConnector, Mockito.times(1)).delete(any());
        Mockito.verifyNoInteractions(rtdProducer);
    }

    @Test
    void testExtractDataForReport() {

        mockRepositoryForReport();

        // Act
        List<ReportExcelDTO> result = selfExpenseService.extractDataForReport(INITIATIVE_ID);


        assertEquals(1, result.size());
        ReportExcelDTO report = result.get(0);

        assertEquals(2, report.getExpenseDataList().size());
        assertEquals("CF", report.get_0_cfGenTutore());

    }

    void mockRepositoryForReport(){
        OnboardingFamilies family = new OnboardingFamilies();
        family.setFamilyId(FAMILY_ID);
        family.setMemberIds(Set.of(USER_ID));
        family.setStatus(OnboardingFamilyEvaluationStatus.ONBOARDING_OK);

        AnprInfo anprInfo = buildAnprInfo();
        SelfDeclarationText selfDeclarationText = new SelfDeclarationText();
        List<SelfDeclarationTextValues> selfDecList = new ArrayList<>();
        SelfDeclarationTextValues selfDecValues1 = new SelfDeclarationTextValues();
        SelfDeclarationTextValues selfDecValues2 = new SelfDeclarationTextValues();
        SelfDeclarationTextValues selfDecValues3 = new SelfDeclarationTextValues();
        selfDecValues1.setValue("Value1");
        selfDecValues2.setValue("Value2");
        selfDecValues3.setValue("Value3");


        selfDecList.add(selfDecValues1);
        selfDecList.add(selfDecValues2);
        selfDecList.add(selfDecValues3);
        selfDeclarationText.setSelfDeclarationTextValues(selfDecList);

        Mockito.when(userFiscalCodeService.getUserFiscalCode(anyString())).thenReturn(Mono.just("CF"));

        ExpenseData expenseData1 = ExpenseData.builder()
                .name(CHILD_NAME)
                .surname(CHILD_SURNAME)
                .amountCents(123456L)
                .expenseDate(LocalDateTime.now())
                .companyName("Centro A")
                .entityId("ABC123")
                .userId(USER_ID)
                .description("Expense description")
                .filesName(Arrays.asList("file1.pdf", "file2.pdf"))
                .build();

        ExpenseData expenseData2 = ExpenseData.builder()
                .name(CHILD_NAME_2)
                .surname(CHILD_SURNAME_2)
                .amountCents(123456L)
                .expenseDate(LocalDateTime.now())
                .companyName("Centro A")
                .entityId("ABC123")
                .userId(USER_ID)
                .description("Expense description")
                .filesName(Arrays.asList("file3.pdf", "file4.pdf"))
                .build();



        Mockito.when(onboardingFamiliesRepository.findByInitiativeId(INITIATIVE_ID))
                .thenReturn(Flux.just(family));

        Mockito.when(anprInfoRepository.findByFamilyId(FAMILY_ID))
                .thenReturn(Mono.just(anprInfo));

        Mockito.when(selfDeclarationTextRepository.findById(anyString()))
                .thenReturn(Mono.just(selfDeclarationText));

        Mockito.when(expenseDataRepository.findByUserId(USER_ID))
                .thenReturn(Flux.just(expenseData1, expenseData2));
    }

    @Test
    void extractFileNameListTest(){

        mockRepositoryForReport();

        Mono<Map<String,String>> fileNameList = selfExpenseService.extractFileNameList(INITIATIVE_ID);

        StepVerifier.create(fileNameList)
                .expectNextMatches(fileNameListE -> {
                    return fileNameListE.size() == 4;
                })
                .verifyComplete();
    }

    @Test
    void generateReportExcelTest(){

        mockRepositoryForReport();

        Mono<ResponseEntity<byte[]>> reportByteMono = selfExpenseService.generateReportExcel(INITIATIVE_ID);

        StepVerifier.create(reportByteMono)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode().value()==200;
                })
                .verifyComplete();

    }

    @Test
    void downloadExpenseFileTest(){

        mockRepositoryForReport();


        Flux<ByteBuffer> expectedResult = Flux.just(ByteBuffer.wrap(new byte[0]));

        Mockito.doReturn(expectedResult)
                .when(fileStorageAsyncConnector)
                .downloadFile(anyString());

        Mono<ResponseEntity<byte[]>> downloadFileMono = selfExpenseService.downloadExpenseFile(INITIATIVE_ID);

        StepVerifier.create(downloadFileMono)
                .expectNextMatches(responseEntity -> {
                    return responseEntity.getStatusCode().value()==200;
                })
                .verifyComplete();

    }


    private static ExpenseDataDTO buildExpenseDataDTO() {


        return ExpenseDataDTO.builder()
                .name("nome")
                .surname("surname")
                .amount(BigDecimal.valueOf(10.20))
                .expenseDate(LocalDateTime.now())
                .companyName("company")
                .entityId("entityId")
                .fiscalCode("ABCQWE89T08H224W")
                .description("initiative")
                .build();

    }





}
