package it.gov.pagopa.self.expense.controller;

import it.gov.pagopa.self.expense.dto.ChildResponseDTO;
import it.gov.pagopa.self.expense.dto.ExpenseDataDTO;
import it.gov.pagopa.self.expense.service.SelfExpenseService;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
public class SelfExpenseControllerImpl implements SelfExpenseController{

    private final SelfExpenseService selfExpenseService;
    public SelfExpenseControllerImpl(SelfExpenseService selfExpenseService) {
        this.selfExpenseService = selfExpenseService;
    }

    @Override
    public Mono<ResponseEntity<ChildResponseDTO>> getChildForUserId(String milAuthToken) {
        return selfExpenseService.getChildForUserId(milAuthToken)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> saveExpenseData(List<FilePart> files, ExpenseDataDTO expenseData) {
        return selfExpenseService.saveExpenseData(files,expenseData)
                .then(Mono.just(ResponseEntity.ok().build()));
    }

    @Override
    public Mono<ResponseEntity<byte[]>> downloadReportExcel(String initiativeId) {

        /*

        1- individuare id iniziativa (serve id come parametro di input)
        2- per quella iniziativa trovare le family
        4- per ogni family estrarre i dati anagr + spese
        5- i documenti allegati dal Blob Storage nel file excel?  wait

        Collection coinvolte:
        initiative, onboarding_families, anpr_info, expense_data, self_declaration_text

        CF del genitore/tutore onboarding_families

        Dichiarazioni (text, multi, ecc…) da chiarire self_declaration_text

        Nucleo componenti (CF sufficiente?) onboarding_families

        Numero figli nel nucleo = anpr_info

        Numero minori nel nucleo = anpr_info

        Residenza = Comune di Guidonia Montecelio

        Spese (con tutti i dettagli) (recuper tutte le info dalla collection expense_data)

        Documenti allegati da recuperare dal Blob Storage (Trovare una strategia per salvarli) nel file excel sarebbe troppo complesso, si può generare uno zip unico con l'alberature per CF

        * */


        return selfExpenseService.generateReportExcel(initiativeId);
    }


}
