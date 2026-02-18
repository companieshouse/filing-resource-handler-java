package uk.gov.companieshouse.filingresourcehandler.serdes;

import org.apache.avro.io.DatumWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingReceived;

@ExtendWith(MockitoExtension.class)
class FilingReceivedSerialiserTest {

    @Mock
    private DatumWriter<FilingReceived> writer;

    @Test
    void testSerialiseMessageSend() {
        // given
        FilingReceived filingReceived = getFilingReceived();

        try (FilingReceivedSerialiser serialiser = new FilingReceivedSerialiser()) {
            // when
            byte[] actual = serialiser.serialize("topic", filingReceived);

            // then
            assertThat(actual, is(notNullValue()));
        }
    }

    @Test
    void testThrowNonRetryableExceptionIfIOExceptionThrown() throws IOException {
        // given
        FilingReceived filingReceived = getFilingReceived();

        FilingReceivedSerialiser serialiser = spy(new FilingReceivedSerialiser());
        when(serialiser.getDatumWriter()).thenReturn(writer);
        doThrow(IOException.class).when(writer).write(any(), any());

        // when
        Executable actual = () -> serialiser.serialize("topic", filingReceived);

        // then
        NonRetryableException exception = assertThrows(NonRetryableException.class, actual);
        assertThat(exception.getMessage(), is(equalTo("Error serialising message payload")));
        assertThat(exception.getCause(), is(instanceOf(IOException.class)));
    }
}