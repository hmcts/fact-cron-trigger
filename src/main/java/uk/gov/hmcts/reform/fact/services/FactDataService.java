package uk.gov.hmcts.reform.fact.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.fact.factdataapi.FactDataClient;

@Service
public class FactDataService {

    private final FactDataClient factDataClient;

    @Autowired
    public FactDataService(FactDataClient factDataClient) {
        this.factDataClient = factDataClient;
    }

    public void createAndUploadCsv() {
        factDataClient.createAndUploadCsv();
    }
}
