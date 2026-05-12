package uk.gov.hmcts.reform.fact.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.factdataapi.FactDataClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class FactDataServiceTest {

    @Mock
    private FactDataClient factDataClient;

    @InjectMocks
    private FactDataService factDataService;

    @Test
    void shouldInvokeFactDataClient() {
        factDataService.createAndUploadCsv();
        verify(factDataClient, times(1)).createAndUploadCsv();
    }
}
