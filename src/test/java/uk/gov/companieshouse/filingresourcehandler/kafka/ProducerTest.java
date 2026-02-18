package uk.gov.companieshouse.filingresourcehandler.kafka;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProducerTest {
    public static final String TEST_TOPIC = "test-topic";
    @Mock
    private KafkaTemplate<String, FilingReceived> kafkaTemplate;
    @Mock
    private MessageFlags messageFlags;
    @Mock
    private FilingReceived filingReceived;
    private Producer producer;
    @BeforeEach
    void setUp() {
        producer = new Producer(kafkaTemplate, TEST_TOPIC, messageFlags);
    }

    @Test
    void publishMessageShouldSendSuccessfully() {
        // Arrange
        CompletableFuture<SendResult<String, FilingReceived>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(TEST_TOPIC, filingReceived)).thenReturn(future);

        // Act
        producer.publishMessage(filingReceived);

        // Assert
        verify(kafkaTemplate).send(TEST_TOPIC, filingReceived);
        verifyNoInteractions(messageFlags);
    }

    @Test
    void publishMessageShouldHandleCompletionException() {
        // Arrange
        doThrow(new CompletionException("Test exception", new RuntimeException()))
                .when(kafkaTemplate).send(TEST_TOPIC, filingReceived);

        // Act & Assert
        assertThatThrownBy(() -> producer.publishMessage(filingReceived))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Completion error during Kafka send Future");

        verify(messageFlags).setRetryable(true);
    }

    @Test
    void publishMessageShouldHandleKafkaException() {
        // Arrange
        doThrow(new KafkaException("Test Kafka exception"))
                .when(kafkaTemplate).send(TEST_TOPIC, filingReceived);

        // Act & Assert
        assertThatThrownBy(() -> producer.publishMessage(filingReceived))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Error publishing to filing-received topic");

        verify(messageFlags).setRetryable(true);
    }
}
