package uk.gov.companieshouse.filingresourcehandler.serdes;

import accounts.transaction_closed;
import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionClosed;

@ExtendWith(MockitoExtension.class)
class TransactionClosedSerialiserTest {
    @Mock
    private DatumWriter<transaction_closed> writer;

    @Test
    void serializeShouldReturnNonNullByteArray() {
        // Arrange
        transaction_closed transactionClosed = getTransactionClosed();

        // Act
        try (TransactionClosedSerialiser serialiser = new TransactionClosedSerialiser()) {

            byte[] serializedData = serialiser.serialize("test-topic", transactionClosed);

            // Assert
            assertThat(serializedData, notNullValue());
        }
    }

    @Test
    void serializeShouldThrowNonRetryableExceptionForInvalidData() throws IOException {
        // Arrange
        transaction_closed transactionClosed = getTransactionClosed();
        TransactionClosedSerialiser serialiser = spy(new TransactionClosedSerialiser());
        when(serialiser.getDatumWriter()).thenReturn(writer);
        doThrow(IOException.class).when(writer).write(any(), any());

        // when
        Executable actual = () -> serialiser.serialize("topic", transactionClosed);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, actual);
        assertThat(exception.getMessage(), is(equalTo("Error serialising message payload")));
        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }
}
