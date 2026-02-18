package uk.gov.companieshouse.filingresourcehandler.factory;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class FilingFactoryTest {

    @Test
    void testGetFilingCreatesCorrectFiling() {
        FilingFactory factory = new FilingFactory();
        FilingApi filingApi = new FilingApi();
        filingApi.setDescription("desc");
        filingApi.setDescriptionIdentifier("id");
        filingApi.setDescriptionValues(new HashMap<>());
        filingApi.setKind("kind");
        filingApi.setCost("10.00");
        String companyNumber = "12345678";
        HashMap<String, String> links = new HashMap<>();
        links.put("Resource", "link");

        Filing filing = factory.getFiling(filingApi, companyNumber, links);

        assertThat(filing.getCompanyNumber()).isEqualTo(companyNumber);
        assertThat(filing.getDescription()).isEqualTo("desc");
        assertThat(filing.getDescriptionIdentifier()).isEqualTo("id");
        assertThat(filing.getDescriptionValues()).isEmpty();
        assertThat(filing.getLinks()).isEqualTo(links);
        assertThat(filing.getStatus()).isEqualTo("processing");
        assertThat(filing.getType()).isEqualTo("kind");
        assertThat(filing.getCost()).isEqualTo("10.00");
    }
}

