package uk.gov.companieshouse.filingresourcehandler.service;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SubmissionIdServiceTest {

    private static final String TRANSACTION_ID = "987654";

    private final SubmissionIdService service = new SubmissionIdService();

    @Test
    void findSubmissionIDOffsetReturnsLargestOffsetAndPopulatesMatcher() {
        Transaction transaction = TestUtils.getTransaction();
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(1);
        assertThat(matcher)
                .containsKey("limited-partnership-post-transition#update-partnership-redesignate-to-pflp:/transactions/987654/limited-partnership/partnership/87qwerty")
                .containsValue("987654-1");
    }

    @Test
    void findSubmissionIDOffsetReturnsZeroWhenFilingsIsNull() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setFilings(null);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isZero();
        assertThat(matcher).isEmpty();
    }

    @Test
    void findSubmissionIDOffsetReturnsZeroWhenFilingsIsEmpty() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        transaction.setFilings(new HashMap<>());
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isZero();
        assertThat(matcher).isEmpty();
    }

    @Test
    void findSubmissionIDOffsetSkipsNullSubmissionId() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();
        filings.put(null, TestUtils.getFiling());
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isZero();
        assertThat(matcher).isEmpty();
    }

    @Test
    void findSubmissionIDOffsetHandlesNullTransactionId() {
        Transaction transaction = new Transaction();
        transaction.setId(null);
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = TestUtils.getFiling();
        filings.put("-1", filing);
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(1);
    }

    @Test
    void findSubmissionIDOffsetTracksLargestOffsetAcrossMultipleFilings() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();

        Filing filing1 = TestUtils.getFiling();
        Map<String, String> links1 = new HashMap<>();
        links1.put("resource", "/transactions/987654/resource/1");
        filing1.setLinks(links1);
        filings.put("987654-1", filing1);

        Filing filing2 = TestUtils.getFiling();
        Map<String, String> links2 = new HashMap<>();
        links2.put("resource", "/transactions/987654/resource/2");
        filing2.setLinks(links2);
        filings.put("987654-3", filing2);

        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(3);
    }

    @Test
    void findSubmissionIDOffsetDoesNotUpdateLargestWhenSmallerOffsetSeen() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();

        Filing filing1 = TestUtils.getFiling();
        filings.put("987654-5", filing1);
        Filing filing2 = TestUtils.getFiling();
        filings.put("987654-2", filing2);

        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(5);
    }

    @Test
    void findSubmissionIDOffsetThrowsRetryableExceptionOnInvalidOffset() {
        Transaction transaction = TestUtils.getTransaction();
        Filing filing = TestUtils.getFiling();
        transaction.getFilings().put("badSubmissionId", filing);
        Map<String, String> matcher = new HashMap<>();
        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class, () -> service.findSubmissionIDOffset(transaction, matcher));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.contains("Invalid offset in submissionID")));
        }
    }

    @Test
    void findSubmissionIDOffsetThrowsRetryableExceptionOnStringIndexOutOfBounds() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID); // length 6
        Map<String, Filing> filings = new HashMap<>();
        filings.put("ab", TestUtils.getFiling()); // length 2, diff=-4, index=2+1+4=7 → SIOOBE
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class, () -> service.findSubmissionIDOffset(transaction, matcher));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.contains("Invalid offset in submissionID")));
        }
    }

    @Test
    void findSubmissionIDOffsetHandlesFilingWithNullType() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = new Filing();
        filing.setType(null);
        Map<String, String> links = new HashMap<>();
        links.put("resource", "/transactions/987654/resource");
        filing.setLinks(links);
        filings.put("987654-2", filing);
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(2);
        assertThat(matcher).containsKey(":/transactions/987654/resource");
    }

    @Test
    void findSubmissionIDOffsetHandlesFilingWithNullLinks() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = new Filing();
        filing.setType("some-type");
        filing.setLinks(null);
        filings.put("987654-2", filing);
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(2);
        assertThat(matcher).containsKey("some-type:");
    }

    @Test
    void findSubmissionIDOffsetHandlesFilingWithMissingResourceInLinks() {
        Transaction transaction = new Transaction();
        transaction.setId(TRANSACTION_ID);
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = new Filing();
        filing.setType("some-type");
        Map<String, String> links = new HashMap<>();
        links.put("other_link", "/some/path");
        filing.setLinks(links);
        filings.put("987654-2", filing);
        transaction.setFilings(filings);
        Map<String, String> matcher = new HashMap<>();
        int offset = service.findSubmissionIDOffset(transaction, matcher);
        assertThat(offset).isEqualTo(2);
        assertThat(matcher).containsKey("some-type:");
    }
}
