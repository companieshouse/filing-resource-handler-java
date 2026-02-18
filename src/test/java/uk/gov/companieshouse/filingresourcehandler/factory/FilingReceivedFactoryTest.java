package uk.gov.companieshouse.filingresourcehandler.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getObjectMapper;

class FilingReceivedFactoryTest {

    @Test
    void testGetFilingReceivedCreatesCorrectObject() {
        String oauthClientId = "client-id";
        FilingReceivedFactory factory = new FilingReceivedFactory(oauthClientId, getObjectMapper());
        Transaction transaction = new Transaction();
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        transaction.setClosedAt("2026-03-03T16:33:00Z");
        transaction.setId("txn-id");
        Map<String, String> closedBy = new HashMap<>();
        closedBy.put("forename", "John");
        closedBy.put("language", "en");
        closedBy.put("surname", "Doe");
        closedBy.put("id", "user-id");
        transaction.setClosedBy(closedBy);
        SubmissionRecord submissionRecord = new SubmissionRecord("12345678", "Test Company", "2026-03-03T16:33:00Z", "txn-id");
        PresenterRecord presenterRecord = new PresenterRecord("John", "en", "Doe", "user-id");
        List<uk.gov.companieshouse.filing.received.Transaction> items = Collections.emptyList();
        transaction.setSubmittedBy(Mockito.mock(uk.gov.companieshouse.api.model.transaction.SubmittedBy.class));
        Mockito.when(transaction.getSubmittedBy().getApplicationId()).thenReturn(oauthClientId);

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);
        assertThat(filingReceived.getChannelId()).isEqualTo("chs");
        assertThat(filingReceived.getPresenter()).usingRecursiveComparison().isEqualTo(presenterRecord);
        assertThat(filingReceived.getSubmission()).usingRecursiveComparison().isEqualTo(submissionRecord);
        assertThat(filingReceived.getItems()).isEqualTo(items);
    }

    @Test
    void getFilingReceivedParsesCompanyNameAndNumberFromItemDataWhenTransactionFieldsEmpty() throws Exception {
        // Arrange
        ObjectMapper objectMapper = getObjectMapper();
        FilingReceivedFactory factory = new FilingReceivedFactory("oauth-client-id", objectMapper);
        uk.gov.companieshouse.filing.received.Transaction item = new uk.gov.companieshouse.filing.received.Transaction();
        String json = "{\"company_number\":\"12345678\",\"company_name\":\"Test Company\"}";
        item.setData(json);
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);
        Transaction transaction = new Transaction();
        transaction.setCompanyName("");
        transaction.setCompanyNumber("");
        transaction.setId("txn-id");
        transaction.setSubmittedBy(new uk.gov.companieshouse.api.model.transaction.SubmittedBy());
        transaction.getSubmittedBy().setApplicationId("oauth-client-id");
        transaction.setClosedBy(Map.of("forename", "John", "language", "en", "surname", "Doe", "id", "user-id"));
        transaction.setClosedAt("2026-03-20T10:00:00Z");
        transaction.setId("txn-id");

        // Act
        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        // Assert
        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEqualTo("12345678");
        assertThat(filingReceived.getSubmission().getCompanyName()).isEqualTo("Test Company");
    }
}
