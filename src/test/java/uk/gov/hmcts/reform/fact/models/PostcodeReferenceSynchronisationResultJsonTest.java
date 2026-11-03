package uk.gov.hmcts.reform.fact.models;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class PostcodeReferenceSynchronisationResultJsonTest {

    @Autowired
    private JacksonTester<PostcodeReferenceSynchronisationResult> json;

    @Test
    void serializesResponseWithExistingJsonFieldNames() throws IOException {
        PostcodeReferenceSynchronisationResult result = result();

        assertThat(json.write(result)).isEqualToJson("""
            {
              "blobUpdated": true,
              "imported": false,
              "sourceVersion": "2026-10",
              "release": "2026.10",
              "recordCount": 123,
              "blobPath": "postcode/reference.csv",
              "importedAt": "2026-10-30T14:00:00Z"
            }
            """);
    }

    @Test
    void deserializesExistingResponseShape() throws IOException {
        PostcodeReferenceSynchronisationResult result = json.parseObject("""
            {
              "blobUpdated": true,
              "imported": false,
              "sourceVersion": "2026-10",
              "release": "2026.10",
              "recordCount": 123,
              "blobPath": "postcode/reference.csv",
              "importedAt": "2026-10-30T14:00:00Z"
            }
            """);

        assertThat(result).isEqualTo(result());
    }

    private PostcodeReferenceSynchronisationResult result() {
        return new PostcodeReferenceSynchronisationResult(
            true,
            false,
            "2026-10",
            "2026.10",
            123,
            "postcode/reference.csv",
            Instant.parse("2026-10-30T14:00:00Z")
        );
    }
}
