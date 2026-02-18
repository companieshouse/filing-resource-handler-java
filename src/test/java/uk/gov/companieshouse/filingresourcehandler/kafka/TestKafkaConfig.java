package uk.gov.companieshouse.filingresourcehandler.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import uk.gov.companieshouse.filingresourcehandler.serdes.TransactionClosedDeserialiser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static uk.gov.companieshouse.filingresourcehandler.kafka.AbstractKafkaIT.CONSUMER_ERROR_TOPIC;
import static uk.gov.companieshouse.filingresourcehandler.kafka.AbstractKafkaIT.CONSUMER_INVALID_TOPIC;
import static uk.gov.companieshouse.filingresourcehandler.kafka.AbstractKafkaIT.CONSUMER_MAIN_TOPIC;
import static uk.gov.companieshouse.filingresourcehandler.kafka.AbstractKafkaIT.CONSUMER_RETRY_TOPIC;


@TestConfiguration
public class TestKafkaConfig {

    @Bean
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

    @Bean
    KafkaProducer<String, byte[]> testProducer(
            @Value("${kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaProducer<>(
                Map.of(
                        ProducerConfig.ACKS_CONFIG, "all",
                        ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers),
                new StringSerializer(), new ByteArraySerializer());
    }
}