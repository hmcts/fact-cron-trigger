package uk.gov.hmcts.reform.fact.runner;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.jobs.FactJob;
import uk.gov.hmcts.reform.fact.jobs.ScheduleTypes;

@Component
@Slf4j
public class FactJobRunner implements CommandLineRunner {

    private final ApplicationContext applicationContext;
    private final ScheduleTypes configuredJobType;

    public FactJobRunner(@Autowired ApplicationContext applicationContext,
                         @Value("${fact.job.type:CSV}") ScheduleTypes configuredJobType) {
        this.applicationContext = applicationContext;
        this.configuredJobType = configuredJobType;
    }

    @Override
    public void run(String... args) {
        FactJob job = applicationContext.getBean(configuredJobType.getJobClass());

        log.info("Selected job '{}'", configuredJobType.name());
        job.execute();
        System.exit(0);
    }
}
