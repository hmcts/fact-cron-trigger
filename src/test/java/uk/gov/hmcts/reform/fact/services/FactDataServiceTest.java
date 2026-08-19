package uk.gov.hmcts.reform.fact.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class FactDataServiceTest {

    @Mock
    private FactDataApiClient factDataApiClient;

    @Test
    void shouldDeleteUsers() {
        FactDataService service = new FactDataService(factDataApiClient);

        service.cleanupUsers();

        verify(factDataApiClient).deleteUsers();
        verifyNoMoreInteractions(factDataApiClient);
    }

    @Test
    void shouldDeleteAudits() {
        FactDataService service = new FactDataService(factDataApiClient);

        service.cleanupAudits();

        verify(factDataApiClient).deleteAudits();
        verifyNoMoreInteractions(factDataApiClient);
    }

    @Test
    void shouldGenerateCsv() {
        FactDataService service = new FactDataService(factDataApiClient);

        service.generateCSV();

        verify(factDataApiClient).createAndUploadCsv();
        verifyNoMoreInteractions(factDataApiClient);
    }
}
