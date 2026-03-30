package uk.gov.companieshouse.filingresourcehandler.factory;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilingReceivedFactoryTest {

    @Mock
    private ObjectMapper objectMapper;


    @Test
    void testGetFilingReceivedCreatesCorrectObject() {
        String oauthClientId = "client-id";
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(oauthClientId);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        SubmissionRecord submissionRecord = TestUtils.getSubmissionRecord();
        PresenterRecord presenterRecord = TestUtils.getPresenterRecord();
        List<uk.gov.companieshouse.filing.received.Transaction> items = TestUtils.getEmptyItemsList();
        transaction.setSubmittedBy(mock(uk.gov.companieshouse.api.model.transaction.SubmittedBy.class));
        when(transaction.getSubmittedBy().getApplicationId()).thenReturn(oauthClientId);

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);
        assertThat(filingReceived.getChannelId()).isEqualTo("chs");
        assertThat(filingReceived.getPresenter()).usingRecursiveComparison().isEqualTo(presenterRecord);
        assertThat(filingReceived.getSubmission()).usingRecursiveComparison().isEqualTo(submissionRecord);
        assertThat(filingReceived.getItems()).isEqualTo(items);
    }

    @Test
    void getFilingReceivedParsesCompanyNameAndNumberFromItemDataWhenTransactionFieldsEmpty() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory("oauth-client-id");
        uk.gov.companieshouse.filing.received.Transaction item = TestUtils.getTransactionItemWithCompanyData();
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setSubmittedBy(new uk.gov.companieshouse.api.model.transaction.SubmittedBy());
        transaction.getSubmittedBy().setApplicationId("oauth-client-id");
        transaction.setClosedBy(TestUtils.getClosedByMap());
        transaction.setClosedAt("2026-03-20T10:00:00Z");
        transaction.setId("txn-id");

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEqualTo("12345678");
        assertThat(filingReceived.getSubmission().getCompanyName()).isEqualTo("Test Company");
    }

    @Test
    void getFilingReceivedThrowsRetryableExceptionWhenJsonParseFails() throws Exception {
        // Arrange
        String oauthClientId = "oauth-client-id";
        FilingReceivedFactory factory = new FilingReceivedFactory(oauthClientId, objectMapper);
        uk.gov.companieshouse.filing.received.Transaction item = TestUtils.getTransactionItemWithCompanyData();
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setSubmittedBy(new uk.gov.companieshouse.api.model.transaction.SubmittedBy());
        transaction.getSubmittedBy().setApplicationId(oauthClientId);
        transaction.setClosedBy(TestUtils.getClosedByMap());
        transaction.setClosedAt("2026-03-20T10:00:00Z");
        transaction.setId("txn-id");
        when(objectMapper.readValue(item.getData(), java.util.Map.class)).thenThrow(new RuntimeException("json error"));
        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString())).thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class, () -> factory.getFilingReceived(items, transaction));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.contains("txn-id")));
        }
    }
}
