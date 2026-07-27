package uk.gov.hmcts.reform.fact.jobs;

public interface FactJob {
    boolean isApplicable(ScheduleTypes scheduleType);

    void execute();
}
