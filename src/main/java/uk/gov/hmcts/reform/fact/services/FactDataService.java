package uk.gov.hmcts.reform.fact.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;

@Service
public class FactDataService {

    private final FactDataApiClient factDataApiClient;

    public FactDataService(@Autowired FactDataApiClient factDataApiClient) {
        this.factDataApiClient = factDataApiClient;
    }

    public void cleanupUsers() {
        factDataApiClient.deleteUsers();
    }

    public void cleanupAudits() {
        factDataApiClient.deleteAudits();
    }

    public void generateCSV() {
        factDataApiClient.createAndUploadCsv();
    }
}

