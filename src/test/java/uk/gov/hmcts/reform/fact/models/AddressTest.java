package uk.gov.hmcts.reform.fact.models;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AddressTest {

    private Address address;
    private List<String> testAddressLines;
    private String testPostcode;
    private String testTown;
    private String testType;
    private String testCounty;
    private String testDescription;
    private String testEpimId;
    private String testFieldsOfLaw;

    @BeforeEach
    void setUp() {
        address = new Address();
        testAddressLines = Arrays.asList("123 Street", "City Center");
        testPostcode = "AB1 2CD";
        testTown = "TestTown";
        testType = "Business";
        testCounty = "TestCounty";
        testDescription = "Main office";
        testEpimId = "EPIM12345";
        testFieldsOfLaw = "Family Law";
    }

    @Test
    void shouldSetAndGetAddressLines() {
        address.setAddressLines(testAddressLines);
        assertThat(address.getAddressLines()).isEqualTo(testAddressLines);
    }

    @Test
    void shouldSetAndGetPostcode() {
        address.setPostcode(testPostcode);
        assertThat(address.getPostcode()).isEqualTo(testPostcode);
    }

    @Test
    void shouldSetAndGetTown() {
        address.setTown(testTown);
        assertThat(address.getTown()).isEqualTo(testTown);
    }

    @Test
    void shouldSetAndGetType() {
        address.setType(testType);
        assertThat(address.getType()).isEqualTo(testType);
    }

    @Test
    void shouldSetAndGetCounty() {
        address.setCounty(testCounty);
        assertThat(address.getCounty()).isEqualTo(testCounty);
    }

    @Test
    void shouldSetAndGetDescription() {
        address.setDescription(testDescription);
        assertThat(address.getDescription()).isEqualTo(testDescription);
    }

    @Test
    void shouldSetAndGetEpimId() {
        address.setEpimId(testEpimId);
        assertThat(address.getEpimId()).isEqualTo(testEpimId);
    }

    @Test
    void shouldSetAndGetFieldsOfLaw() {
        address.setFieldsOfLaw(testFieldsOfLaw);
        assertThat(address.getFieldsOfLaw()).isEqualTo(testFieldsOfLaw);
    }
}

