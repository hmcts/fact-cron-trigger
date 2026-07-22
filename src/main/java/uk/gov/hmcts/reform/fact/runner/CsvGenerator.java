package uk.gov.hmcts.reform.fact.runner;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.services.AzureService;
import uk.gov.hmcts.reform.fact.services.FactDataService;
import uk.gov.hmcts.reform.fact.services.FactService;

@Component
@Slf4j
@Profile("!test")
public class CsvGenerator implements CommandLineRunner {

    private final AzureService azureService;
    private final FactService factService;
    private final FactDataService factDataService;

    public CsvGenerator(@Autowired AzureService azureService,
                        @Autowired FactService factService,
                        @Autowired FactDataService factDataService) {
        this.azureService = azureService;
        this.factService = factService;
        this.factDataService = factDataService;
    }

    /**
     * Executes automatically when the Spring Boot application starts.
     * This method is invoked as part of the application's startup process due to
     * the implementation of the {@link CommandLineRunner} interface. At present, it triggers
     * the generation of court and tribunal data in CSV format and uploads it to
     * Azure Blob Storage. The method logs the start and completion of this process.
     *
     * @param args Optional command-line arguments passed during application startup.
     */
    @Override
    public void run(String... args) {
        log.info("Running CSV generation");
        createCsvAndUpload(); // Uses fact api
        factDataService.createAndUploadCsv(); // Uses fact data api
        log.info("Finished running CSV generation");
        System.exit(0);
    }

    public void createCsvAndUpload() {
        JsonNode courtData = factService.getCourtData();
        azureService.createCsvFileAndUpload("csv", "courts-and-tribunals-data.csv", courtData);
    }
}
