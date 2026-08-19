package uk.gov.hmcts.reform.fact.factapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import uk.gov.hmcts.reform.fact.config.FactDataApiFeignConfiguration;

@FeignClient(name = "factDataApi", url = "${fact-data-api.url}", configuration = FactDataApiFeignConfiguration.class)
public interface FactDataApiClient {


    @DeleteMapping("${fact-data-api.endpoint.delete-users}")
    void deleteUsers();

    @DeleteMapping("${fact-data-api.endpoint.delete-audits}")
    void deleteAudits();

    @PostMapping("${fact-data-api.endpoint.create-and-upload-csv}")
    String createAndUploadCsv();
}

