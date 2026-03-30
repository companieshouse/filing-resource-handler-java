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
    void findSubmissionIDOffsetThrowsRetryableExceptionOnInvalidOffset() {
        Transaction transaction = TestUtils.getTransaction();
        // Add a malformed submissionID
        Filing filing = TestUtils.getFiling();
        transaction.getFilings().put("badSubmissionId", filing);
        Map<String, String> matcher = new HashMap<>();
        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString())).thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class, () -> service.findSubmissionIDOffset(transaction, matcher));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.contains("Invalid offset in submissionID")));
        }
    }
}

