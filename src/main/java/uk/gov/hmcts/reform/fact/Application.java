package uk.gov.hmcts.reform.fact;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(clients = {uk.gov.hmcts.reform.fact.factapi.FactClient.class,
    uk.gov.hmcts.reform.fact.factdataapi.FactDataClient.class})
@SuppressWarnings("HideUtilityClassConstructor") // Spring needs a constructor, it's not a utility class
public class Application {

    public static void main(final String[] args) {
        SpringApplication.run(Application.class, args);
        System.exit(0);
    }
}
