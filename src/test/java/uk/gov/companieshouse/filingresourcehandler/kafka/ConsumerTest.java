package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.service.FilingResourceHandlerService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsumerTest {
    @Mock
    private FilingResourceHandlerService filingResourceHandlerService;
    @Mock
    private MessageFlags messageFlags;
    @Mock
    private transaction_closed transactionClosed;
    @Mock
    private Message<transaction_closed> message;
    @InjectMocks
    private Consumer consumer;

    @Test
    void consumeShouldProcessMessage() {


        when(message.getPayload()).thenReturn(transactionClosed);

        consumer.consume(message);

        verify(filingResourceHandlerService).processMessage(transactionClosed);
        verifyNoInteractions(messageFlags);
    }

    @Test
    void consumeShouldSetRetryableFlagAndRethrowOnRetryableException() {
        when(message.getPayload()).thenReturn(transactionClosed);

        RetryableException exception = new RetryableException("retryable");
        doThrow(exception).when(filingResourceHandlerService).processMessage(transactionClosed);

        Throwable thrown = catchThrowable(() -> consumer.consume(message));

        assertThat(thrown).isSameAs(exception);
        verify(messageFlags).setRetryable(true);
    }
}
