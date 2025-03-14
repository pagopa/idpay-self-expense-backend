package it.gov.pagopa.self.expense.service;

import it.gov.pagopa.common.reactive.pdv.service.UserFiscalCodeService;
import it.gov.pagopa.self.expense.configuration.ExceptionMap;
import it.gov.pagopa.self.expense.connector.FileStorageAsyncConnector;
import it.gov.pagopa.self.expense.constants.Constants;
import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.event.producer.RtdProducer;
import it.gov.pagopa.self.expense.model.AnprInfo;
import it.gov.pagopa.self.expense.model.Child;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.model.mapper.ExpenseDataMapper;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.utils.MockFilePart;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    @MockBean
    private FileStorageAsyncConnector fileStorageAsyncConnector;

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
