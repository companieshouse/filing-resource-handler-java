package uk.gov.companieshouse.filingresourcehandler.kafka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.concurrent.CompletionException;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;


@Component
public class Producer {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final KafkaTemplate<String, FilingReceived> kafkaTemplate;
    private final String filingReceivedTopic;
    private final MessageFlags messageFlags;

    public Producer(KafkaTemplate<String, FilingReceived> kafkaTemplate,
                    @Value("${message.send.topic}") String filingReceivedTopic, MessageFlags messageFlags) {
        this.kafkaTemplate = kafkaTemplate;
        this.filingReceivedTopic = filingReceivedTopic;
        this.messageFlags = messageFlags;
    }

    public void publishMessage(FilingReceived filingReceived) {
        try {
            kafkaTemplate.send(filingReceivedTopic, filingReceived).join();
        } catch (CompletionException ex) {
            final String msg = "Completion error during Kafka send Future";
            LOGGER.error(msg, DataMapHolder.getLogMap());
            messageFlags.setRetryable(true);
            throw new RetryableException(msg, ex);
        } catch (KafkaException ex) {
            final String msg = "Error publishing to filing-received topic";
            LOGGER.error(msg, DataMapHolder.getLogMap());
            messageFlags.setRetryable(true);
            throw new RetryableException(msg, ex);
        }
        LOGGER.info("Successfully published message to filing-received topic", DataMapHolder.getLogMap());
    }
}