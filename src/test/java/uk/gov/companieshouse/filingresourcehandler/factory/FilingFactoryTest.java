package uk.gov.companieshouse.filingresourcehandler.factory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

class FilingFactoryTest {

    private static final String COMPANY_NUMBER = "12345678";

    @Test
    void testGetFilingCreatesCorrectFiling() {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        HashMap<String, String> links = TestUtils.getLinks();

        Filing filing = factory.getFiling(filingApi, COMPANY_NUMBER, links);

        assertThat(filing.getCompanyNumber()).isEqualTo(COMPANY_NUMBER);
        assertThat(filing.getDescription()).isEqualTo("desc");
        assertThat(filing.getDescriptionIdentifier()).isEqualTo("id");
        assertThat(filing.getDescriptionValues()).isEmpty();
        assertThat(filing.getLinks()).isEqualTo(links);
        assertThat(filing.getStatus()).isEqualTo("processing");
        assertThat(filing.getType()).isEqualTo("test-kind");
        assertThat(filing.getCost()).isEqualTo("10.00");
    }

    @Test
    void testGetFilingDefaultsDescriptionToEmptyStringWhenNull() {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        filingApi.setDescription(null);

        Filing filing = factory.getFiling(filingApi, COMPANY_NUMBER, TestUtils.getLinks());

        assertThat(filing.getDescription()).isEmpty();
    }

    @Test
    void testGetFilingDefaultsDescriptionIdentifierToEmptyStringWhenNull() {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        filingApi.setDescriptionIdentifier(null);

        Filing filing = factory.getFiling(filingApi, COMPANY_NUMBER, TestUtils.getLinks());

        assertThat(filing.getDescriptionIdentifier()).isEmpty();
    }

    @Test
    void testGetFilingDefaultsTypeToEmptyStringWhenKindIsNull() {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        filingApi.setKind(null);

        Filing filing = factory.getFiling(filingApi, COMPANY_NUMBER, TestUtils.getLinks());

        assertThat(filing.getType()).isEmpty();
    }

    @Test
    void testGetFilingDefaultsCostToEmptyStringWhenNull() {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        filingApi.setCost(null);

        Filing filing = factory.getFiling(filingApi, COMPANY_NUMBER, TestUtils.getLinks());

        assertThat(filing.getCost()).isEmpty();
    }
}
