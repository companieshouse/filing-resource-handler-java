package uk.gov.companieshouse.filingresourcehandler.factory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class FilingReceivedFactoryTest {

    private static final String CLIENT_ID = "client-id";
    private static final String OAUTH_CLIENT_ID = "oauth-client-id";

    @Mock
    private ObjectMapper objectMapper;


    @Test
    void testGetFilingReceivedCreatesCorrectObject() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        SubmissionRecord submissionRecord = TestUtils.getSubmissionRecord();
        PresenterRecord presenterRecord = TestUtils.getPresenterRecord();
        List<uk.gov.companieshouse.filing.received.Transaction> items = TestUtils.getEmptyItemsList();
        transaction.setSubmittedBy(mock(uk.gov.companieshouse.api.model.transaction.SubmittedBy.class));
        when(transaction.getSubmittedBy().getApplicationId()).thenReturn(CLIENT_ID);

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);
        assertThat(filingReceived.getChannelId()).isEqualTo("chs");
        assertThat(filingReceived.getPresenter()).usingRecursiveComparison().isEqualTo(presenterRecord);
        assertThat(filingReceived.getSubmission()).usingRecursiveComparison().isEqualTo(submissionRecord);
        assertThat(filingReceived.getItems()).isEqualTo(items);
    }

    @Test
    void getFilingReceivedParsesCompanyNameAndNumberFromItemDataWhenTransactionFieldsEmpty() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(OAUTH_CLIENT_ID);
        uk.gov.companieshouse.filing.received.Transaction item = TestUtils.getTransactionItemWithCompanyData();
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setSubmittedBy(new uk.gov.companieshouse.api.model.transaction.SubmittedBy());
        transaction.getSubmittedBy().setApplicationId(OAUTH_CLIENT_ID);
        transaction.setClosedBy(TestUtils.getClosedByMap());
        transaction.setClosedAt("2026-03-20T10:00:00Z");
        transaction.setId("txn-id");

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEqualTo("12345678");
        assertThat(filingReceived.getSubmission().getCompanyName()).isEqualTo("Test Company");
    }

    @Test
    void getFilingReceivedThrowsRetryableExceptionWhenJsonParseFails() throws Exception {
        FilingReceivedFactory factory = new FilingReceivedFactory(OAUTH_CLIENT_ID, objectMapper);
        uk.gov.companieshouse.filing.received.Transaction item = TestUtils.getTransactionItemWithCompanyData();
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setSubmittedBy(new uk.gov.companieshouse.api.model.transaction.SubmittedBy());
        transaction.getSubmittedBy().setApplicationId(OAUTH_CLIENT_ID);
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

    @Test
    void getFilingReceivedAllowsNullCompanyNumberWhenJsonHasNoCompanyNumberKey() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(OAUTH_CLIENT_ID);

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setCompanyNumber(null);
        transaction.setCompanyName("Test Company");

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEmpty();
        assertThat(filingReceived.getSubmission().getCompanyName()).isEqualTo("Test Company");
    }

    @Test
    void getFilingReceivedThrowsWhenTransactionIsNull() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(OAUTH_CLIENT_ID);
        List<uk.gov.companieshouse.filing.received.Transaction> items = TestUtils.getEmptyItemsList();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> factory.getFilingReceived(items, null))
                .isInstanceOf(uk.gov.companieshouse.filingresourcehandler.exception.RetryableException.class)
                .hasMessageContaining("Transaction is null");
    }

    @Test
    void getFilingReceivedThrowsWhenFirstItemDataIsNullAndCompanyFieldsBlank() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(OAUTH_CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-null-data");

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class,
                    () -> factory.getFilingReceived(items, transaction));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(
                    Mockito.contains("no data")));
        }
    }

    @Test
    void getFilingReceivedUsesApiFilingChannelWhenApplicationIdDiffers() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        uk.gov.companieshouse.api.model.transaction.SubmittedBy submittedBy =
                new uk.gov.companieshouse.api.model.transaction.SubmittedBy();
        submittedBy.setApplicationId("a-different-app");
        transaction.setSubmittedBy(submittedBy);

        FilingReceived filingReceived = factory.getFilingReceived(
                TestUtils.getEmptyItemsList(), transaction);

        assertThat(filingReceived.getChannelId()).isEqualTo("api-filing");
        assertThat(filingReceived.getApplicationId()).isEqualTo("a-different-app");
    }

    @Test
    void getFilingReceivedHandlesNullSubmittedByAndNullClosedBy() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setSubmittedBy(null);
        transaction.setClosedBy(null);
        FilingReceived filingReceived = factory.getFilingReceived(
                TestUtils.getEmptyItemsList(), transaction);

        assertThat(filingReceived.getApplicationId()).isEmpty();
        assertThat(filingReceived.getChannelId()).isEqualTo("api-filing");
        assertThat(filingReceived.getPresenter().getForename()).isEmpty();
        assertThat(filingReceived.getPresenter().getSurname()).isEmpty();
        assertThat(filingReceived.getPresenter().getLanguage()).isEmpty();
        assertThat(filingReceived.getPresenter().getUserId()).isEmpty();
    }

    @Test
    void getFilingReceivedHandlesNullTransactionIdWhenFieldsPopulated() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setId(null);

        FilingReceived filingReceived = factory.getFilingReceived(
                TestUtils.getEmptyItemsList(), transaction);

        assertThat(filingReceived.getSubmission().getTransactionId()).isEmpty();
    }

    @Test
    void getFilingReceivedKeepsBlankFieldsWhenJsonHasNoCompanyKeys() throws Exception {
        FilingReceivedFactory factory = new FilingReceivedFactory(OAUTH_CLIENT_ID, objectMapper);

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-no-keys");
        transaction.setClosedBy(TestUtils.getClosedByMap());

        when(objectMapper.readValue(Mockito.eq("{}"), Mockito.<com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>>any()))
                .thenReturn(java.util.Map.of("other_field", "x"));

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyName()).isEmpty();
        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEmpty();
    }

    @Test
    void getFilingReceivedTreatsNullReadValueAsEmptyMap() throws Exception {
        FilingReceivedFactory factory = new FilingReceivedFactory(OAUTH_CLIENT_ID, objectMapper);

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-null-map");
        transaction.setClosedBy(TestUtils.getClosedByMap());

        when(objectMapper.readValue(Mockito.eq("{}"), Mockito.<com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>>any()))
                .thenReturn(null);

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyName()).isEmpty();
        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEmpty();
    }

    @Test
    void getFilingReceivedSetsClosedAtToEmptyStringWhenNull() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setClosedAt(null);

        FilingReceived filingReceived = factory.getFilingReceived(TestUtils.getEmptyItemsList(), transaction);

        assertTrue(filingReceived.getSubmission().getReceivedAt().isEmpty());
    }

    @Test
    void getFilingReceivedSetsClosedAtToEmptyStringWhenBlank() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setClosedAt("   ");

        FilingReceived filingReceived = factory.getFilingReceived(TestUtils.getEmptyItemsList(), transaction);

        assertTrue(filingReceived.getSubmission().getReceivedAt().isEmpty());
    }

    @Test
    void getFilingReceivedSetsClosedAtWhenPresent() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(CLIENT_ID);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setClosedAt("2026-03-03T16:33:00Z");

        FilingReceived filingReceived = factory.getFilingReceived(TestUtils.getEmptyItemsList(), transaction);
        assertEquals("2026-03-03T16:33:00Z", filingReceived.getSubmission().getReceivedAt());
    }
}
