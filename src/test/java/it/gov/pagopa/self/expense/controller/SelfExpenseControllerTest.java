package it.gov.pagopa.self.expense.controller;


import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.model.Child;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import it.gov.pagopa.self.expense.utils.MockFilePart;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;



@WebFluxTest(SelfExpenseController.class)
class SelfExpenseControllerTest {

    private static final String USER_ID = "userId";
    private static final String MIL_AUTH_TOKEN = "milAuthToken";
    private static final String CHILD_NAME = "nome";
    private static final String CHILD_SURNAME = "cognome";

    @MockBean
    private SelfExpenseService selfExpenseService;

    @Autowired
    private WebTestClient webClient;

    @Test
    void shouldReturnChildList_WhenValidUserIdAndInitiativeId() {
        // Given
        var expectedResponse = buildChildResponseDTO();

        Mockito.when(selfExpenseService.getChildForUserId(MIL_AUTH_TOKEN))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webClient.get()
                .uri("/idpay/self-expense/get-child/{milAuthToken}", MIL_AUTH_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ChildResponseDTO.class)
                .consumeWith(response -> {
                    var actualResponse = response.getResponseBody();
                    Assertions.assertNotNull(actualResponse);
                    Assertions.assertEquals(expectedResponse, actualResponse);
                });
    }

    @NotNull
    private static ChildResponseDTO buildChildResponseDTO() {
        var child = new Child();
        child.setCognome(CHILD_SURNAME);
        child.setUserId(USER_ID);
        child.setNome(CHILD_NAME);

        var childResponseDTO = new ChildResponseDTO();
        childResponseDTO.setChildList(List.of(child));

        return childResponseDTO;
    }



    void shouldReturnVoid_WhenExpenseDataAreSavedCorrectly() {

        List<FilePart> files = MockFilePart.generateMockFileParts();

        ExpenseDataDTO dto = ExpenseDataDTO.builder()
                .name("nome")
                .surname("surname")
                .amount(BigDecimal.valueOf(10.20))
                .expenseDate(LocalDateTime.now())
                .companyName("company")
                .entityId("entityId")
                .fiscalCode("ABCQWE89T08H224W")
                .description("initiative")
                .build();

        Mockito.when(selfExpenseService.saveExpenseData(files, dto))
                .thenReturn(Mono.empty());

        // When & Then
        webClient.post()
                .uri("/idpay/self-expense/save-expense-data")
                .bodyValue(dto)
                .exchange()
                .expectStatus().isOk()
                ;
    }

}
