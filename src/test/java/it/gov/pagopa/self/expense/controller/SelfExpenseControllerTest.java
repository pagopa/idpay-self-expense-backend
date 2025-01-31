package it.gov.pagopa.self.expense.controller;


import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.model.Child;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;



@WebFluxTest(SelfExpenseController.class)
class SelfExpenseControllerTest {

    private static final String USER_ID = "userId";
    private static final String INITIATIVE_ID = "initiative";
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

        Mockito.when(selfExpenseService.getChildForUserId(USER_ID, INITIATIVE_ID))
                .thenReturn(Mono.just(expectedResponse));

        // When & Then
        webClient.get()
                .uri("/idpay/self-expense/get-child/{userId}/{initiativeId}", USER_ID, INITIATIVE_ID)
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
}
