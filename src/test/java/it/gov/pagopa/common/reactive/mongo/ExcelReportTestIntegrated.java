package it.gov.pagopa.common.reactive.mongo;

import it.gov.pagopa.common.mongo.MongoTestIntegrated;
import it.gov.pagopa.self.expense.enums.OnboardingFamilyEvaluationStatus;
import it.gov.pagopa.self.expense.model.AnprInfo;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.model.OnboardingFamilies;
import it.gov.pagopa.self.expense.model.SelfDeclarationText;
import it.gov.pagopa.self.expense.repository.AnprInfoRepository;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.repository.OnboardingFamiliesRepository;
import it.gov.pagopa.self.expense.repository.SelfDeclarationTextRepository;
import it.gov.pagopa.self.expense.service.SelfExpenseServiceImpl;
import it.gov.pagopa.self.expense.utils.excel.ExcelPOIHelper;
import org.aspectj.lang.annotation.Before;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ContextConfiguration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * See confluence page: <a href="https://pagopa.atlassian.net/wiki/spaces/IDPAY/pages/615974424/Secrets+UnitTests">Secrets for UnitTests</a>
 */
@SuppressWarnings({"squid:S3577", "NewClassNamingConvention"}) // suppressing class name not match alert: we are not using the Test suffix in order to let not execute this test by default maven configuration because it depends on properties not pushable. See
@MongoTestIntegrated
//@ContextConfiguration(classes = {SelfExpenseServiceImpl.class})
class ExcelReportTestIntegrated extends BaseReactiveMongoRepositoryIntegrationTest {

    @Autowired
    private ReactiveMongoTemplate mongoTemplate;


    //@Autowired
    //private SelfExpenseServiceImpl selfExpenseService;

    @Autowired
    private OnboardingFamiliesRepository onboardingFamiliesRepository;

    @Autowired
    private SelfDeclarationTextRepository selfDeclarationTextRepository;

    @Autowired
    private AnprInfoRepository anprInfoRepository;

    @Autowired
    private ExpenseDataRepository expenseDataRepository;

    @Test
    void testFindFamiliesById() {

        final String INITIATIVE_ID = "67caef1b22580100a70ee47a";
        final String USER_ID = "008b4308-7101-45e2-af03-04cd36f459b2";

        System.out.println("__");
        Flux<OnboardingFamilies> families = onboardingFamiliesRepository.findByInitiativeId(INITIATIVE_ID)
                .filter(family -> OnboardingFamilyEvaluationStatus.ONBOARDING_OK.equals(family.getStatus()));
        // Log each element in families
        families.doOnNext(family -> System.out.println("OnboardingFamily: "+ family))
                .collectList().block();

        System.out.println("__");
        Mono<SelfDeclarationText> selfDeclaration = selfDeclarationTextRepository.findById(SelfDeclarationText.buildId(INITIATIVE_ID, USER_ID));
        selfDeclaration.doOnNext(declaration -> System.out.println("SelfDeclaration: "+ declaration)).block();

        System.out.println("__");
        Mono<AnprInfo> anprInfo = anprInfoRepository.findByUserId(USER_ID);
        anprInfo.doOnNext(anpr -> System.out.println("AnprInfo: "+ anpr)).block();

        System.out.println("__");
        Flux<ExpenseData> expenseData = expenseDataRepository.findByUserId(USER_ID);
        List<ExpenseData> expenseDataList = expenseData.collectList().block(); // Colleziona i dati in una lista
        System.out.println("ExpenseDataList: "+ expenseDataList);
        // Aggiungi l'asserzione per verificare che expenseData sia popolata
        Assertions.assertNotNull(expenseDataList, "La lista di ExpenseData non dovrebbe essere null.");
        Assertions.assertFalse(expenseDataList.isEmpty(), "La lista di ExpenseData dovrebbe essere popolata.");

        //Start Excel report





    }

    @Test
    void generateExcelFile() throws IOException {


        String path = "C:\\IdPay\\tmp\\";
        String FILE_NAME = "temp.xlsx";
        String fileLocation = path + FILE_NAME;

        List<String> headerNames = Arrays.
                asList("Col1","Col2", "Col3");

        List<List<String>> rowValues = new ArrayList<>();

        List<String> row1 = Arrays.asList("valu1","value2","value3");
        List<String> row2 = Arrays.asList("valu4","value5","value6");
        rowValues.add(row1);
        rowValues.add(row2);
        ExcelPOIHelper excelPOIHelper = new ExcelPOIHelper();
        excelPOIHelper.writeExcel(headerNames, rowValues);


        Map<Integer, List<String>> data = excelPOIHelper.readExcel(fileLocation);

        assertEquals(headerNames.get(0), data.get(0)
                .get(0));
    }

    @Test
    void testFindByField() {
        // Supponiamo che tu abbia un'entità chiamata YourEntity con un campo "name"
        String expectedFamily = "Test_Family_1";

        // Creare un'istanza di YourEntity e salvarla nel database
        AnprInfo entity = new AnprInfo();
        entity.setUserId("test1");
        entity.setFamilyId(expectedFamily);
        entity.setInitiativeId("initId");
        mongoTemplate.save(entity).block();

        // Creare una query per trovare l'entità per nome
        Query query = new Query();
        query.addCriteria(Criteria.where("familyId").is(expectedFamily));

        // Eseguire la query
        Mono<AnprInfo> resultMono = mongoTemplate.findOne(query, AnprInfo.class);

        // Verificare il risultato
        AnprInfo result = resultMono.block();
        Assertions.assertNotNull(result);
        assertEquals(expectedFamily, result.getFamilyId());

        mongoTemplate.remove(query, AnprInfo.class).block();

        // Verificare che l'oggetto sia stato rimosso
        AnprInfo deletedResult = mongoTemplate.findOne(query, AnprInfo.class).block();
        Assertions.assertNull(deletedResult, "L'oggetto non è stato rimosso dal database.");







    }

    private String getCodFiscFromUserId(String userId){

        Map<String, String> codFiscMap = new HashMap<>();
        codFiscMap.put("008b4308-7101-45e2-af03-04cd36f459b2", "CF_PADRE");
        codFiscMap.put("5dbc50f8-19fa-4095-84c4-d42f8555ed48", "CF_FIGLIO_1");
        codFiscMap.put("7e9d97ee-a133-4c9f-ba48-13c9f9bb1de9", "CF_FIGLIO_2");
        return codFiscMap.get(userId);
    }



}
