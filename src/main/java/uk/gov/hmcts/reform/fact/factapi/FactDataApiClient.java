package uk.gov.hmcts.reform.fact.factapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import uk.gov.hmcts.reform.fact.config.FactDataApiFeignConfiguration;

import java.util.Map;

@FeignClient(name = "factDataApi", url = "${fact-data-api.url}", configuration = FactDataApiFeignConfiguration.class)
public interface FactDataApiClient {

    @PostMapping("${fact-data-api.endpoint.user}")
    Map<String, Object> createOrUpdateUser(@RequestBody Map<String, String> userPayload);

    @DeleteMapping("${fact-data-api.endpoint.delete-users}")
    void deleteUsers();

    @DeleteMapping("${fact-data-api.endpoint.delete-audits}")
    void deleteAudits(@RequestHeader("X-User-Id") String userId);
}

