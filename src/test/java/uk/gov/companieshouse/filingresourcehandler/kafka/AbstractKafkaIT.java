package uk.gov.companieshouse.filingresourcehandler.kafka;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.collect.Iterables;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import uk.gov.companieshouse.filing.received.FilingReceived;

import java.io.ByteArrayOutputStream;
import java.time.Duration;

@Testcontainers
@Import(TestKafkaConfig.class)
@WireMockTest(httpPort = 8889)
abstract class AbstractKafkaIT {

    protected static final String CONSUMER_MAIN_TOPIC = "tx-closed";
    protected static final String CONSUMER_RETRY_TOPIC = "tx-closed-filing-resource-handler-retry";
    protected static final String CONSUMER_ERROR_TOPIC = "tx-closed-filing-resource-handler-error";
    protected static final String CONSUMER_INVALID_TOPIC = "tx-closed-filing-resource-handler-invalid";
    @Container
    protected static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:latest");
    @Autowired
    protected KafkaConsumer<String, byte[]> testConsumer;
    @Autowired
    protected KafkaProducer<String, byte[]> testProducer;
    @Autowired
    protected TestConsumerAspect testConsumerAspect;
    @Autowired
    protected KafkaTemplate<String, FilingReceived> kafkaTemplate;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.poll(Duration.ofMillis(1000));
    }

    protected static int recordsPerTopic(ConsumerRecords<?, ?> records, String topic) {
        return Iterables.size(records.records(topic));
    }

    protected static <T> byte[] writePayloadToBytes(T data, Class<T> type) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            Encoder encoder = EncoderFactory.get().directBinaryEncoder(outputStream, null);
            DatumWriter<T> writer = new ReflectDatumWriter<>(type);
            writer.write(data, encoder);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}