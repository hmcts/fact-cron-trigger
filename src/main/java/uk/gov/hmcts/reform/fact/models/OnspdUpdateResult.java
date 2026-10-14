package uk.gov.hmcts.reform.fact.models;

public record OnspdUpdateResult(
    boolean updated,
    String release,
    long recordCount,
    String blobPath
) {

    public static OnspdUpdateResult unchanged(String release, String blobPath) {
        return new OnspdUpdateResult(false, release, 0, blobPath);
    }

    public static OnspdUpdateResult updated(String release, long recordCount, String blobPath) {
        return new OnspdUpdateResult(true, release, recordCount, blobPath);
    }
}
