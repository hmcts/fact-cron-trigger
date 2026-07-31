package uk.gov.hmcts.reform.fact.jobs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.reform.fact.integrations.SlackMessageHelper;
import uk.gov.hmcts.reform.fact.integrations.SlackNotificationConstants;
import uk.gov.hmcts.reform.fact.services.CsvGenerationService;

import java.util.Optional;

@Component
@Slf4j
public class CsvFactJob implements FactJob {

    private final CsvGenerationService csvGenerationService;
    private final SlackMessageHelper slackMessageHelper;

    public CsvFactJob(@Autowired CsvGenerationService csvGenerationService,
                      @Autowired SlackMessageHelper slackMessageHelper) {
        this.csvGenerationService = csvGenerationService;
        this.slackMessageHelper = slackMessageHelper;
    }

    @Override
    public boolean isApplicable(ScheduleTypes scheduleType) {
        return ScheduleTypes.CSV == scheduleType;
    }

    @Override
    public void execute() {
        log.info("Running CSV generation job");
        SlackNotificationConstants notification = SlackNotificationConstants.CSV;
        try {
            csvGenerationService.createCsvAndUpload();
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getSuccessIcon(),
                Optional.empty()
            );
            log.info("Finished CSV generation job");
        } catch (RuntimeException ex) {
            slackMessageHelper.sendDailyCheckSummary(
                notification.getServiceName(),
                notification.getFailureIcon(),
                Optional.of(notification.getFailurePrefix() + ex.getMessage())
            );
            throw ex;
        }
    }
}
