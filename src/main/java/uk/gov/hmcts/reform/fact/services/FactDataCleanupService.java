package uk.gov.hmcts.reform.fact.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.factapi.FactDataApiClient;

@Service
public class FactDataCleanupService {

    private final FactDataApiClient factDataApiClient;

    public FactDataCleanupService(@Autowired FactDataApiClient factDataApiClient) {
        this.factDataApiClient = factDataApiClient;
    }

    public void cleanupUsers() {
        factDataApiClient.deleteUsers();
    }

    public void cleanupAudits() {
        factDataApiClient.deleteAudits();
    }
}

