package uk.gov.hmcts.reform.fact.services;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.gov.hmcts.reform.fact.factapi.FactClient;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest()
class CsvCreationIntTest {

    @Autowired
    private AzureService azureService;
    @Autowired
    private BlobServiceClient blobServiceClient;
    @Autowired
    private FactClient factClient;
    @Autowired
    private FactService factService;

    /**
     * Determine that the @SpringBootTest() annotation when launched created the new updated file in the
     * AAT SA. Since it takes a minute or so for the build to get to this stage, we can safely assume
     * it would be new if it was replaced within a minute. This means we don't have to delete/readd
     * each time for the test.
     */
    @Test
    void determineNewCsvAddedToSA() {
        BlobProperties props = blobServiceClient.getBlobContainerClient("csv")
            .getBlobClient("courts-and-tribunals-data.csv")
            .getProperties();

        OffsetDateTime lastModified = props.getLastModified();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        long secondsDiff = Duration.between(lastModified, now).abs().getSeconds();

        System.out.println("CSV Details. Last modified: " + lastModified + " | Now: " + now);
        Assertions.assertTrue(secondsDiff <= 60, "Blob not created within the last minute");
    }

    /**
     *
     * To determine if the information retrieved from the API matches that of what
     * is deserialised and converted to the models. I.e. determine if anything is missing.
     * Will prevent data being omitted from the spreadsheet.
     *
     * @throws JsonProcessingException Failure to deserialise the returning JsonNode
     */
    @Test
    void compareFactAPIWithMethodToConvertToModel() throws JsonProcessingException {
        // Get the Data from Fact Api endpoint
        String rawApiCourtData = factClient.getAllCourtData();

        // Get the converted model from the Fact Service.
        JsonNode convertedModelList = factService.getCourtData();

        // Make sure they both equal one another in terms of content.
        // If not, then the conversion with the model classes has missed something or another variable has been
        // added in the response to the call to FACT API
        assertThat(
            StringUtils.difference(
                rawApiCourtData,
                new ObjectMapper().writeValueAsString(convertedModelList)
            )).isEmpty();
    }
}
