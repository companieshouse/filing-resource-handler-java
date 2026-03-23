package uk.gov.companieshouse.filingresourcehandler.serdes;

import accounts.transaction_closed;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.avro.specific.SpecificDatumWriter;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uk.gov.companieshouse.filingresourcehandler.exception.InvalidPayloadException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionClosedMessage;

class TransactionClosedDeserialiserTest {

    @Test
    void testShouldSuccessfullyDeserializeMessageReceived() throws IOException {
        transaction_closed messageSend = getTransactionClosedMessage();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<transaction_closed> writer = new ReflectDatumWriter<>(transaction_closed.class);
        writer.write(messageSend, encoder);
        try (TransactionClosedDeserialiser deserialiser = new TransactionClosedDeserialiser()) {
            transaction_closed actual = deserialiser.deserialize("topic", outputStream.toByteArray());
            assertThat(actual, is(equalTo(messageSend)));
        }
    }

    @Test
    void testDeserializeThrowsInvalidException() throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<String> writer = new SpecificDatumWriter<>(String.class);
        writer.write("hello", encoder);
        try (TransactionClosedDeserialiser deserialiser = new TransactionClosedDeserialiser()) {
            Executable actual = () -> deserialiser.deserialize("topic", outputStream.toByteArray());
            InvalidPayloadException exception = assertThrows(InvalidPayloadException.class, actual);
            assertThat(exception.getMessage(), is(equalTo("Invalid payload: [\nhello] was provided")));
            assertThat(exception.getCause(), is(CoreMatchers.instanceOf(IOException.class)));
        }
    }
}
