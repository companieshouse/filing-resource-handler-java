package uk.gov.companieshouse.filingresourcehandler.kafka;

import java.time.Duration;

import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.google.common.collect.Iterables;

public final class KafkaUtils {

    static final String MAIN_TOPIC = "message-send";
    static final String RETRY_TOPIC = "message-send-message-send-consumer-group-retry";
    static final String ERROR_TOPIC = "message-send-message-send-consumer-group-error";
    static final String INVALID_TOPIC = "message-send-message-send-consumer-group-invalid";

    private KafkaUtils() {
    }

    static int noOfRecordsForTopic(ConsumerRecords<?, ?> records, String topic) {
        return Iterables.size(records.records(topic));
    }

    static Duration kafkaPollingDuration() {
        String kafkaPollingDuration = System.getenv().containsKey("KAFKA_POLLING_DURATION") ?
                System.getenv("KAFKA_POLLING_DURATION") : "1000";
        return Duration.ofMillis(Long.parseLong(kafkaPollingDuration));
    }

}