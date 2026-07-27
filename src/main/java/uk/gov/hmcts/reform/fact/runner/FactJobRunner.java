package uk.gov.hmcts.reform.fact.runner;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.config.CronTimerProperties;
import uk.gov.hmcts.reform.fact.jobs.FactJob;
import uk.gov.hmcts.reform.fact.jobs.ScheduleTypes;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class FactJobRunner implements CommandLineRunner {

    private final List<? extends FactJob> jobs;
    private final CronTimerProperties cronTimerProperties;

    public FactJobRunner(@Autowired List<? extends FactJob> jobs,
                         @Autowired CronTimerProperties cronTimerProperties) {
        this.jobs = jobs;
        this.cronTimerProperties = cronTimerProperties;
    }

    @Override
    public void run(String... args) {
        if (!cronTimerProperties.isEnabled()) {
            log.warn("Trigger runner is disabled for '{}'.", cronTimerProperties.getTriggerType());
            System.exit(1);
            return;
        }

        ScheduleTypes configuredJobType = EnumUtils.getEnum(ScheduleTypes.class, cronTimerProperties.getTriggerType());
        if (configuredJobType == null) {
            log.error("Invalid or no schedule type set. Exiting");
            System.exit(1);
            return;
        }

        Optional<? extends FactJob> selectedJob = jobs.stream()
            .filter(job -> job.isApplicable(configuredJobType))
            .findFirst();

        selectedJob.ifPresentOrElse(job -> {
            log.info("Selected job '{}'", configuredJobType.name());
            job.execute();
            System.exit(0);
        }, () -> {
            log.error("No job bean found for schedule type '{}'. Exiting", configuredJobType.name());
            System.exit(1);
        });
    }


}
