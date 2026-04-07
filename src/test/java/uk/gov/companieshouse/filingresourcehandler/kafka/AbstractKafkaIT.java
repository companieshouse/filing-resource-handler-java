package uk.gov.companieshouse.filingresourcehandler.kafka;

import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import com.google.common.collect.Iterables;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Encoder;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.reflect.ReflectDatumWriter;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import uk.gov.companieshouse.filingresourcehandler.serdes.TransactionClosedDeserialiser;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Testcontainers
@WireMockTest(httpPort = 8889)
abstract class AbstractKafkaIT {

    protected static final String CONSUMER_MAIN_TOPIC = "tx-closed";
    protected static final String CONSUMER_RETRY_TOPIC = "tx-closed-filing-resource-handler-retry";
    protected static final String CONSUMER_ERROR_TOPIC = "tx-closed-filing-resource-handler-error";
    protected static final String CONSUMER_INVALID_TOPIC = "tx-closed-filing-resource-handler-invalid";
    @Container
    protected static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:latest");
    protected KafkaConsumer<String, byte[]> testConsumer = testConsumer(kafka.getBootstrapServers());

    protected KafkaProducer<String, byte[]> testProducer = testProducer(kafka.getBootstrapServers());
    @Autowired
    protected TestConsumerAspect testConsumerAspect;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @BeforeEach
    protected void setup() {
        testConsumerAspect.resetLatch();
        testConsumer.subscribe(getSubscribedTopics());
        testConsumer.poll(Duration.ofMillis(1000));
    }

    protected List<String> getSubscribedTopics() {
        return List.of(CONSUMER_MAIN_TOPIC, CONSUMER_RETRY_TOPIC, CONSUMER_ERROR_TOPIC, CONSUMER_INVALID_TOPIC);
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

    KafkaConsumer<String, byte[]> testConsumer(@Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        KafkaConsumer<String, byte[]> consumer = new KafkaConsumer<>(
                Map.of(
                        ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers,
                        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class,
                        ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, StringDeserializer.class,
                        ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, TransactionClosedDeserialiser.class,
                        ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest",
                        ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false",
                        ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString()),
                new StringDeserializer(), new ByteArrayDeserializer());
        consumer.subscribe(List.of(CONSUMER_MAIN_TOPIC, CONSUMER_RETRY_TOPIC, CONSUMER_ERROR_TOPIC, CONSUMER_INVALID_TOPIC));
        return consumer;
    }


    KafkaProducer<String, byte[]> testProducer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaProducer<>(
                Map.of(
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                new StringSerializer(), new ByteArraySerializer());
    }
}