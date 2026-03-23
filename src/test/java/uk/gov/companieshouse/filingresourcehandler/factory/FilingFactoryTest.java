package uk.gov.companieshouse.filingresourcehandler.factory;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class FilingFactoryTest {

    @Test
    void testGetFilingCreatesCorrectFiling() throws JsonProcessingException {
        FilingFactory factory = TestUtils.getFilingFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        String companyNumber = "12345678";
        HashMap<String, String> links = TestUtils.getLinks();

        Filing filing = factory.getFiling(filingApi, companyNumber, links);

        assertThat(filing.getCompanyNumber()).isEqualTo(companyNumber);
        assertThat(filing.getDescription()).isEqualTo("desc");
        assertThat(filing.getDescriptionIdentifier()).isEqualTo("id");
        assertThat(filing.getDescriptionValues()).isEmpty();
        assertThat(filing.getLinks()).isEqualTo(links);
        assertThat(filing.getStatus()).isEqualTo("processing");
        assertThat(filing.getType()).isEqualTo("test-kind");
        assertThat(filing.getCost()).isEqualTo("10.00");
    }
}
