package uk.gov.companieshouse.filingresourcehandler.serdes;


import accounts.transaction_closed;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class TransactionClosedSerialiser implements Serializer<transaction_closed> {
    @Override
    public byte[] serialize(String topic, transaction_closed data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<transaction_closed> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException e) {
            throw new NonRetryableException("Error serialising message payload", e);
        }
        return outputStream.toByteArray();
    }

    public DatumWriter<transaction_closed> getDatumWriter() {
        return new ReflectDatumWriter<>(transaction_closed.class);
    }
}
