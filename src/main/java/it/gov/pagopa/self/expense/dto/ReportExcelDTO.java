package it.gov.pagopa.self.expense.dto;

import it.gov.pagopa.self.expense.model.ExpenseData;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class ReportExcelDTO {

    public static final List<String> headerName = Arrays.asList(
            "CF Genitore/Tutore",
            "Dichiarazioni",
            "CF componenti nucleo",
            "N.  minori nel nucleo",
            "N. figli minori",
            "Residenza",
            "Importo Richiesto",
            "Dettaglio Spese",
            "Nome File Doc di spese"
    );

    private String _0_cfGenTutore;
    private String _1_dichiarazioni;
    private String _2_CF_compNucleo;
    private String _3_N_minoriNucleo;
    private String _4_N_figliMinori;
    private final String _5_residenza = "Comune di Guidonia Montecelio";
    private List<ExpenseData> expenseDataList;


    public List<List<String>> mapToRowValues() {
        List<List<String>> result = new ArrayList<>();
        final String expenseDetail = """
                Nome: %s
                Cognome: %s
                Data: %s
                Nome ente: %s
                Descrizione: %s
                Codice ente: %s
                """;
        final String fileNameTemplate = "%s_%s";
        String fileNameAsList = "";
        List<String> singleRowValues = new ArrayList<>(headerName.size());
        for(ExpenseData expenseData : expenseDataList) {

            singleRowValues.add(_0_cfGenTutore);
            singleRowValues.add(_1_dichiarazioni);
            singleRowValues.add(_2_CF_compNucleo);
            singleRowValues.add(_3_N_minoriNucleo);
            singleRowValues.add(_4_N_figliMinori);
            singleRowValues.add(_5_residenza);
            singleRowValues.add((expenseData.getAmountCents()==null?null:Long.toString(expenseData.getAmountCents()/100L)));
            singleRowValues.add(String.format(expenseDetail,
                            expenseData.getName(), expenseData.getSurname(), expenseData.getExpenseDate(), expenseData.getCompanyName(),
                            expenseData.getDescription(), expenseData.getEntityId()
                    ));

            fileNameAsList = expenseData.getFilesName().stream()
                    .map(s -> String.format(fileNameTemplate, _0_cfGenTutore, s))
                    .collect(Collectors.joining("\n"));

            singleRowValues.add(fileNameAsList);
            result.add(singleRowValues);

        }

        return result;
    }

}