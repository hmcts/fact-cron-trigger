package uk.gov.hmcts.reform.fact.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AreaOfLawTest {

    private AreaOfLaw areaOfLaw;

    @BeforeEach
    void setUp() {
        areaOfLaw = new AreaOfLaw();
    }

    @Test
    void testSetAndGetFields() {
        areaOfLaw.setName("Family Law");
        areaOfLaw.setExternalLink("https://example.com/family-law");
        areaOfLaw.setDisplayUrl("https://display.example.com");
        areaOfLaw.setExternalLinkDesc("This is an external link description.");
        areaOfLaw.setDisplayName("Family Law Cases");
        areaOfLaw.setDisplayExternalLink("https://example.com/display-family-law");

        assertThat(areaOfLaw.getName()).isEqualTo("Family Law");
        assertThat(areaOfLaw.getExternalLink()).isEqualTo("https://example.com/family-law");
        assertThat(areaOfLaw.getDisplayUrl()).isEqualTo("https://display.example.com");
        assertThat(areaOfLaw.getExternalLinkDesc()).isEqualTo("This is an external link description.");
        assertThat(areaOfLaw.getDisplayName()).isEqualTo("Family Law Cases");
        assertThat(areaOfLaw.getDisplayExternalLink()).isEqualTo("https://example.com/display-family-law");
    }

    @Test
    void testDefaultValuesAreNull() {
        AreaOfLaw newAreaOfLaw = new AreaOfLaw();
        assertThat(newAreaOfLaw.getName()).isNull();
        assertThat(newAreaOfLaw.getExternalLink()).isNull();
        assertThat(newAreaOfLaw.getDisplayUrl()).isNull();
        assertThat(newAreaOfLaw.getExternalLinkDesc()).isNull();
        assertThat(newAreaOfLaw.getDisplayName()).isNull();
        assertThat(newAreaOfLaw.getDisplayExternalLink()).isNull();
    }
}

