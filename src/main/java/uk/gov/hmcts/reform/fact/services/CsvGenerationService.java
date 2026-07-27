package uk.gov.hmcts.reform.fact.services;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CsvGenerationService {

    private final AzureService azureService;
    private final FactService factService;

    public CsvGenerationService(@Autowired AzureService azureService,
                                @Autowired FactService factService) {
        this.azureService = azureService;
        this.factService = factService;
    }

    public void createCsvAndUpload() {
        JsonNode courtData = factService.getCourtData();
        azureService.createCsvFileAndUpload("csv", "courts-and-tribunals-data.csv", courtData);
    }
}

