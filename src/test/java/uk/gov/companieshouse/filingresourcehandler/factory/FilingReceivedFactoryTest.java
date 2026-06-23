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
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void getFilingReceivedAllowsNullCompanyNumberWhenJsonHasNoCompanyNumberKey() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory("oauth-client-id");

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setCompanyNumber(null);
        transaction.setCompanyName("Test Company");

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertEquals("", filingReceived.getSubmission().getCompanyNumber());
        assertThat(filingReceived.getSubmission().getCompanyName()).isEqualTo("Test Company");
    }

    @Test
    void getFilingReceivedThrowsWhenTransactionIsNull() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory("oauth-client-id");
        List<uk.gov.companieshouse.filing.received.Transaction> items = TestUtils.getEmptyItemsList();

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> factory.getFilingReceived(items, null))
                .isInstanceOf(uk.gov.companieshouse.filingresourcehandler.exception.RetryableException.class)
                .hasMessageContaining("Transaction is null");
    }


    @Test
    void getFilingReceivedThrowsWhenFirstItemDataIsNullAndCompanyFieldsBlank() {
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory("oauth-client-id");
        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-null-data");

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction(); // data == null
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
        String oauthClientId = "client-id";
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(oauthClientId);
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
        String oauthClientId = "client-id";
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(oauthClientId);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setSubmittedBy(null);
        transaction.setClosedBy(null);
        FilingReceived filingReceived = factory.getFilingReceived(
                TestUtils.getEmptyItemsList(), transaction);

        assertThat(filingReceived.getApplicationId()).isEmpty();
        assertThat(filingReceived.getChannelId()).isEqualTo("api-filing");
        assertThat(filingReceived.getPresenter().getForename()).isNull();
        assertThat(filingReceived.getPresenter().getSurname()).isNull();
        assertThat(filingReceived.getPresenter().getLanguage()).isNull();
        assertThat(filingReceived.getPresenter().getUserId()).isNull();
    }

    @Test
    void getFilingReceivedHandlesNullTransactionIdWhenFieldsPopulated() {
        String oauthClientId = "client-id";
        FilingReceivedFactory factory = TestUtils.getFilingReceivedFactory(oauthClientId);
        Transaction transaction = TestUtils.getTransactionForFilingReceived();
        transaction.setId(null);

        FilingReceived filingReceived = factory.getFilingReceived(
                TestUtils.getEmptyItemsList(), transaction);

        assertThat(filingReceived.getSubmission().getTransactionId()).isEmpty();
    }

    @Test
    void getFilingReceivedKeepsBlankFieldsWhenJsonHasNoCompanyKeys() throws Exception {
        String oauthClientId = "oauth-client-id";
        FilingReceivedFactory factory = new FilingReceivedFactory(oauthClientId, objectMapper);

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-no-keys");
        transaction.setClosedBy(TestUtils.getClosedByMap());

        // Return a map with neither company_number nor company_name -> exercises null branches
        when(objectMapper.readValue(Mockito.eq("{}"), Mockito.<com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>>any()))
                .thenReturn(java.util.Map.of("other_field", "x"));

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyName()).isEmpty();
        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEmpty();
    }

    @Test
    void getFilingReceivedTreatsNullReadValueAsEmptyMap() throws Exception {
        String oauthClientId = "oauth-client-id";
        FilingReceivedFactory factory = new FilingReceivedFactory(oauthClientId, objectMapper);

        uk.gov.companieshouse.filing.received.Transaction item =
                new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(item);

        Transaction transaction = TestUtils.getTransactionWithEmptyCompanyFields();
        transaction.setId("txn-null-map");
        transaction.setClosedBy(TestUtils.getClosedByMap());

        // Force readValue to return null -> exercises Optional.ofNullable(...).orElse(Map.of())
        when(objectMapper.readValue(Mockito.eq("{}"), Mockito.<com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>>any()))
                .thenReturn(null);

        FilingReceived filingReceived = factory.getFilingReceived(items, transaction);

        assertThat(filingReceived.getSubmission().getCompanyName()).isEmpty();
        assertThat(filingReceived.getSubmission().getCompanyNumber()).isEmpty();
    }
}
