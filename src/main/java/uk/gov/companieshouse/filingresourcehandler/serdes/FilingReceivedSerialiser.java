package uk.gov.companieshouse.filingresourcehandler.serdes;

import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.common.serialization.Serializer;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class FilingReceivedSerialiser implements Serializer<FilingReceived> {


    @Override
    public byte[] serialize(String topic, FilingReceived data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
        DatumWriter<FilingReceived> writer = getDatumWriter();
        try {
            writer.write(data, encoder);
        } catch (IOException e) {
            throw new NonRetryableException("Error serialising message payload", e);
        }
        return outputStream.toByteArray();
    }

    public DatumWriter<FilingReceived> getDatumWriter() {
        return new ReflectDatumWriter<>(FilingReceived.class);
    }
}
