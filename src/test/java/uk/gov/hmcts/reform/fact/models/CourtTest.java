package uk.gov.hmcts.reform.fact.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CourtTest {

    private Court court;

    @BeforeEach
    void setUp() {
        court = new Court();
    }

    @Test
    void testSetAndGetFields() {
        List<String> types = Arrays.asList("Family", "Criminal");
        List<AreaOfLaw> areasOfLaw = Arrays.asList(new AreaOfLaw(), new AreaOfLaw());
        List<String> areasOfLawSPOE = Arrays.asList("SPOE1", "SPOE2");
        List<Address> addresses = Arrays.asList(new Address(), new Address());

        court.setName("High Court");
        court.setLat(51.5074);
        court.setLon(-0.1278);
        court.setNumber(123);
        court.setCciCode(456);
        court.setMagistrateCode(789);
        court.setSlug("high-court");
        court.setTypes(types);
        court.setAreasOfLaw(areasOfLaw);
        court.setAreasOfLawSPOE(areasOfLawSPOE);
        court.setDisplayed(true);
        court.setHideAols(false);
        court.setDxNumber("DX12345");
        court.setDistance(10.5);
        court.setAddresses(addresses);

        assertThat(court.getName()).isEqualTo("High Court");
        assertThat(court.getLat()).isEqualTo(51.5074);
        assertThat(court.getLon()).isEqualTo(-0.1278);
        assertThat(court.getNumber()).isEqualTo(123);
        assertThat(court.getCciCode()).isEqualTo(456);
        assertThat(court.getMagistrateCode()).isEqualTo(789);
        assertThat(court.getSlug()).isEqualTo("high-court");
        assertThat(court.getTypes()).isEqualTo(types);
        assertThat(court.getAreasOfLaw()).isEqualTo(areasOfLaw);
        assertThat(court.getAreasOfLawSPOE()).isEqualTo(areasOfLawSPOE);
        assertThat(court.isDisplayed()).isTrue();
        assertThat(court.isHideAols()).isFalse();
        assertThat(court.getDxNumber()).isEqualTo("DX12345");
        assertThat(court.getDistance()).isEqualTo(10.5);
        assertThat(court.getAddresses()).isEqualTo(addresses);
    }

    @Test
    void testDefaultValuesAreNullOrFalse() {
        Court newCourt = new Court();
        assertThat(newCourt.getName()).isNull();
        assertThat(newCourt.getLat()).isNull();
        assertThat(newCourt.getLon()).isNull();
        assertThat(newCourt.getNumber()).isNull();
        assertThat(newCourt.getCciCode()).isNull();
        assertThat(newCourt.getMagistrateCode()).isNull();
        assertThat(newCourt.getSlug()).isNull();
        assertThat(newCourt.getTypes()).isNull();
        assertThat(newCourt.getAreasOfLaw()).isNull();
        assertThat(newCourt.getAreasOfLawSPOE()).isNull();
        assertThat(newCourt.isDisplayed()).isFalse();
        assertThat(newCourt.isHideAols()).isFalse();
        assertThat(newCourt.getDxNumber()).isNull();
        assertThat(newCourt.getDistance()).isNull();
        assertThat(newCourt.getAddresses()).isNull();
    }
}
