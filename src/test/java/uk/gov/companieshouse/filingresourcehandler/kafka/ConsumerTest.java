package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.service.FilingResourceHandlerService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsumerTest {

    private FilingResourceHandlerService filingResourceHandlerService;
    private MessageFlags messageFlags;
    private Consumer consumer;

    @BeforeEach
    void setUp() {
        filingResourceHandlerService = mock(FilingResourceHandlerService.class);
        messageFlags = mock(MessageFlags.class);
        consumer = new Consumer(filingResourceHandlerService, messageFlags);
    }

    @Test
    void consumeShouldProcessMessage() {
        transaction_closed transactionClosed = mock(transaction_closed.class);
        Message<transaction_closed> message = mock(Message.class);
        when(message.getPayload()).thenReturn(transactionClosed);

        consumer.consume(message);

        verify(filingResourceHandlerService).processMessage(transactionClosed);
        verifyNoInteractions(messageFlags);
    }

    @Test
    void consumeShouldSetRetryableFlagAndRethrowOnRetryableException() {
        transaction_closed transactionClosed = mock(transaction_closed.class);
        Message<transaction_closed> message = mock(Message.class);
        when(message.getPayload()).thenReturn(transactionClosed);

        RetryableException exception = new RetryableException("retryable");
        doThrow(exception).when(filingResourceHandlerService).processMessage(transactionClosed);

        Throwable thrown = catchThrowable(() -> consumer.consume(message));

        assertThat(thrown).isSameAs(exception);
        verify(messageFlags).setRetryable(true);
    }
}