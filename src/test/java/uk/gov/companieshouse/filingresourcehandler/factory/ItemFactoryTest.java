package uk.gov.companieshouse.filingresourcehandler.factory;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filing.received.Transaction;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class ItemFactoryTest {

    @Test
    void testGetItemCreatesCorrectTransaction() {
        ItemFactory factory = TestUtils.getItemFactory();
        FilingApi filingApi = TestUtils.getFilingApi();
        String submissionId = "sub123";
        String filingDataJson = "{\"key\":\"value\"}";

        Transaction item = factory.getItem(filingApi, submissionId, filingDataJson);

        assertThat(item.getData()).isEqualTo(filingDataJson);
        assertThat(item.getKind()).isEqualTo("test-kind");
        assertThat(item.getSubmissionId()).isEqualTo(submissionId);
        assertThat(item.getSubmissionLanguage()).isEqualTo("en");
    }
}
