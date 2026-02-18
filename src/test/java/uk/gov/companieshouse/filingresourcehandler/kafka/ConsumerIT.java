package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.service.FilingResourceHandlerService;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingApi;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getObjectMapper;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionClosed;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionRequestBody;

@SpringBootTest
class ConsumerIT extends AbstractKafkaIT {
    @Mock
    private FilingResourceHandlerService filingResourceHandlerService;
    @Mock
    private MessageFlags messageFlags;
    @Mock
    private transaction_closed transactionClosed;
    @Mock
    private Message<transaction_closed> message;
    @InjectMocks
    private Consumer consumer;
    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    @Test
    void shouldConsumeTransactionClosedMessagesAndProcessSuccessfully() throws JsonProcessingException, InterruptedException {
        // given
        byte[] message = writePayloadToBytes(getTransactionClosed(), transaction_closed.class);
        Transaction transaction = getTransactionRequestBody();
        ObjectMapper objectMapper = getObjectMapper();

        String transactionString = objectMapper.writeValueAsString(transaction);
        stubFor(get("/private/transactions/987654")
                .willReturn(aResponse()
                        .withStatus(200).withBody(transactionString)));

        stubFor(patch("/private/transactions/987654?force=true")
                .willReturn(aResponse()
                        .withStatus(204)));

        stubFor(get(urlPathMatching("/private/transactions/[^/]+/limited-partnership/partnership/[^/]+/filings"))
                .withHeader("X-Request-Id", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(getFilingApi())));

        // when
        testProducer.send(new ProducerRecord<>(CONSUMER_MAIN_TOPIC, 0, System.currentTimeMillis(), "key", message));
        if (!testConsumerAspect.getLatch().await(500, TimeUnit.SECONDS)) {
            fail("Timed out waiting for latch");
        }
        // then
        ConsumerRecords<?, ?> consumerRecords = KafkaTestUtils.getRecords(testConsumer, Duration.ofMillis(10000L), 1);
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_MAIN_TOPIC)).isOne();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_RETRY_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_ERROR_TOPIC)).isZero();
        assertThat(recordsPerTopic(consumerRecords, CONSUMER_INVALID_TOPIC)).isZero();
        WireMock.verify(1, getRequestedFor(urlEqualTo("/private/transactions/987654")));
        WireMock.verify(0, patchRequestedFor(urlEqualTo("/private/transactions/987654?force=true")));
        WireMock.verify(1, getRequestedFor(urlPathMatching("/private/transactions/[^/]+/limited-partnership/partnership/[^/]+/filings"))
                .withQueryParam("resource", matching(".*"))
                .withQueryParam("company_name", matching(".*"))
                .withQueryParam("company_number", matching(".*")));
    }

    @Test
    void consumeShouldProcessMessage() {


        when(message.getPayload()).thenReturn(transactionClosed);

        consumer.consume(message);

        verify(filingResourceHandlerService).processMessage(transactionClosed);
        verifyNoInteractions(messageFlags);
    }

    @Test
    void consumeShouldSetRetryableFlagAndRethrowOnRetryableException() {
        when(message.getPayload()).thenReturn(transactionClosed);

        RetryableException exception = new RetryableException("retryable");
        doThrow(exception).when(filingResourceHandlerService).processMessage(transactionClosed);

        Throwable thrown = catchThrowable(() -> consumer.consume(message));

        assertThat(thrown).isSameAs(exception);
        verify(messageFlags).setRetryable(true);
    }
}