package uk.gov.hmcts.reform.fact.jobs;

public enum ScheduleTypes {
    CSV(CsvFactJob.class),
    USER_CLEANUP(UserCleanupJob.class),
    AUDIT_CLEANUP(AuditCleanupJob.class);

    private final Class<? extends FactJob> jobClass;

    ScheduleTypes(Class<? extends FactJob> jobClass) {
        this.jobClass = jobClass;
    }

    public Class<? extends FactJob> getJobClass() {
        return jobClass;
    }
}
