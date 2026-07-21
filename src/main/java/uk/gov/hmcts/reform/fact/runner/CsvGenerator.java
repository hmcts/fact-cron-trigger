package uk.gov.hmcts.reform.fact.runner;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.services.AzureService;
import uk.gov.hmcts.reform.fact.services.FactService;

@Component
@Lazy
public class CsvGenerator {

    private final AzureService azureService;
    private final FactService factService;

    public CsvGenerator(@Autowired AzureService azureService,
                        @Autowired FactService factService) {
        this.azureService = azureService;
        this.factService = factService;
    }

    public void createCsvAndUpload() {
        JsonNode courtData = factService.getCourtData();
        azureService.createCsvFileAndUpload("csv", "courts-and-tribunals-data.csv", courtData);
    }
}
