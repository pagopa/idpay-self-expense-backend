package it.gov.pagopa.self.expense.service.commands;

import com.mongodb.MongoException;
import it.gov.pagopa.self.expense.model.ExpenseData;
import it.gov.pagopa.self.expense.repository.ExpenseDataRepository;
import it.gov.pagopa.self.expense.utils.AuditUtilities;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DeleteInitiativeServiceImplTest {

    @Mock private ExpenseDataRepository expenseDataRepository;
    @Mock private AuditUtilities auditUtilitiesMock;

    private DeleteInitiativeService deleteInitiativeService;
    private final static int PAGE_SIZE = 100;


    @BeforeEach
    void setUp() {
        deleteInitiativeService = new DeleteInitiativeServiceImpl(
                expenseDataRepository,
                auditUtilitiesMock,
                PAGE_SIZE, 1000L);
    }

    @Test
    void executeOK() {
        String initiativeId = "INITIATIVEID";

        ExpenseData expenseData = ExpenseData.builder()
                .id("id")
                .userId("userId")
                .description("description")
                .build();

        Mockito.when(expenseDataRepository.findAllWithBatch(PAGE_SIZE))
                .thenReturn(Flux.just(expenseData));

        Mockito.when(expenseDataRepository.deleteById(expenseData.getId()))
                .thenReturn(Mono.empty());

        String result = deleteInitiativeService.execute(initiativeId).block();

        Assertions.assertNotNull(result);

        Mockito.verify(expenseDataRepository, Mockito.times(1)).findAllWithBatch(Mockito.anyInt());
        Mockito.verify(expenseDataRepository, Mockito.times(1)).deleteById(Mockito.anyString());

    }

    @Test
    void executeError() {
        String initiativeId = "INITIATIVEID";
        Mockito.when(expenseDataRepository.findAllWithBatch(PAGE_SIZE))
                .thenThrow(new MongoException("DUMMY_EXCEPTION"));

        try {
            deleteInitiativeService.execute(initiativeId).block();
        }catch (Exception e){

            Assertions.assertTrue(e instanceof MongoException);
        }
    }
}