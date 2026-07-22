package uk.gov.hmcts.reform.fact.runner;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.services.AzureService;
import uk.gov.hmcts.reform.fact.services.FactDataService;
import uk.gov.hmcts.reform.fact.services.FactService;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CsvGeneratorTest {

    @Mock
    private AzureService azureService;

    @Mock
    private FactService factService;

    @Mock
    private FactDataService factDataService;

    private CsvGenerator csvGenerator;

    @BeforeEach
    void setUp() {
        csvGenerator = new CsvGenerator(azureService, factService, factDataService);
    }

    @Test
    void shouldCreateCsvAndUpload() {
        JsonNode mockData = JsonNodeFactory.instance.objectNode();
        when(factService.getCourtData()).thenReturn(mockData);

        csvGenerator.createCsvAndUpload();

        verify(factService).getCourtData();
        verify(azureService).createCsvFileAndUpload(eq("csv"), eq("courts-and-tribunals-data.csv"), eq(mockData));
    }
}
