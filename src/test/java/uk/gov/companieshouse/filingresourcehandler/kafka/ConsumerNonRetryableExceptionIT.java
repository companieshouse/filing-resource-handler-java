package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionClosed;

@SpringBootTest
class ConsumerNonRetryableExceptionIT extends AbstractKafkaIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Test
    void testRepublishToTransactionClosedInvalidMessageTopicIfNonRetryableExceptionThrown() throws Exception {
        // given
        transaction_closed transactionClosed = getTransactionClosed();
        byte[] message = writePayloadToBytes(transactionClosed, transaction_closed.class);

        stubFor(get("/private/transactions/987654")
                .willReturn(aResponse()
                        .withStatus(400)));

        // when
        testProducer.send(new ProducerRecord<>(CONSUMER_MAIN_TOPIC, 0, System.currentTimeMillis(), "key", message));
        if (!testConsumerAspect.getLatch().await(60L, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }

        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 2);
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_MAIN_TOPIC)).isOne();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_INVALID_TOPIC)).isOne();
        verify(getRequestedFor(urlEqualTo("/private/transactions/987654")));
    }
}
