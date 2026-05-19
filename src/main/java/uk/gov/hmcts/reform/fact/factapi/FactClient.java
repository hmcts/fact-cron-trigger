package uk.gov.hmcts.reform.fact.factapi;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.reform.fact.config.FeignFactConfig;

@FeignClient(name = "factApi", url = "${fact.url}", configuration = FeignFactConfig.class)
public interface FactClient {

    @GetMapping("${fact.endpoint.all-court-data}")
    String getAllCourtData();
}
