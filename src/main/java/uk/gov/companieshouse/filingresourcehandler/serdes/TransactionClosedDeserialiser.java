package uk.gov.companieshouse.filingresourcehandler.serdes;

import accounts.transaction_closed;
import org.apache.avro.AvroRuntimeException;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.reflect.ReflectDatumReader;
import org.apache.kafka.common.serialization.Deserializer;
import uk.gov.companieshouse.filingresourcehandler.exception.InvalidPayloadException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.io.IOException;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

public class TransactionClosedDeserialiser implements Deserializer<transaction_closed> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    @Override
    public transaction_closed deserialize(String topic, byte[] data) {
        try {
            Decoder decoder = DecoderFactory.get().binaryDecoder(data, null);
            DatumReader<transaction_closed> reader = new ReflectDatumReader<>(transaction_closed.class);
            return reader.read(null, decoder);
        } catch (IOException | AvroRuntimeException e) {
            LOGGER.error("Error deserialising filing received payload ", e);
            throw new InvalidPayloadException(("Invalid payload: [%s] was provided".formatted(new String(data))), e);
        }
    }
}
