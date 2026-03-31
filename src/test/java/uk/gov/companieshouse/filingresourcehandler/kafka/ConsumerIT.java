package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import uk.gov.companieshouse.api.model.transaction.Transaction;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.patch;
import static com.github.tomakehurst.wiremock.client.WireMock.patchRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.TEST_TRANSACTIONS_KEY;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingApiList;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getObjectMapper;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransaction;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransactionClosedMessage;

@SpringBootTest
class ConsumerIT extends AbstractKafkaIT {

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("steps", () -> 1);
    }

    // Regex patterns for different filing API paths
    private static final String LIMITED_PARTNERSHIP_REGEX = "^/private/transactions/[^/]+/limited-partnership/partnership/[^/]+/filings$";
    private static final String PSC_VERIFICATION_REGEX = "^/private/transactions/[^/]+/persons-with-significant-control-verification/[^/]+/filings$";
    private static final String ACSP_APPLICATIONS_REGEX = "^/private/transactions/[^/]+/authorised-corporate-service-provider-applications/[^/]+/filings$";
    private static final String ACCOUNTS_REGEX = "^/private/transactions/[^/]+/accounts/[^/]+/filings$";


    private static Stream<Arguments> filingApiPaths() {
        return Stream.of(
                Arguments.of(
                        "/transactions/987654/persons-with-significant-control-verification/87qwerty",
                        PSC_VERIFICATION_REGEX
                ),
                Arguments.of(
                        "/transactions/987654/authorised-corporate-service-provider-applications/87qwerty",
                        ACSP_APPLICATIONS_REGEX
                ),
                Arguments.of(
                        "/transactions/987654/accounts/87qwerty=",
                        ACCOUNTS_REGEX
                )
        );
    }

    @Test
    void shouldConsumeTransactionClosedMessagesAndProcessSuccessfullyNoPatch(
    ) throws Exception {
        // given
        byte[] message = writePayloadToBytes(getTransactionClosedMessage(), transaction_closed.class);
        Transaction transaction = getTransaction();
        Map<String, String> resourceLinks;
        resourceLinks = transaction.getResources().get(TEST_TRANSACTIONS_KEY).getLinks();
        resourceLinks.put("resource", TEST_TRANSACTIONS_KEY);
        transaction.getResources().get(TEST_TRANSACTIONS_KEY).setLinks(resourceLinks);
        ObjectMapper objectMapper = getObjectMapper();

        String transactionString = objectMapper.writeValueAsString(transaction);
        stubFor(get("/private/transactions/987654")
                .willReturn(aResponse()
                        .withStatus(200).withBody(transactionString)));

        stubFor(patch(urlPathEqualTo("/private/transactions/987654"))
                .withQueryParam("force", matching("true"))
                .willReturn(aResponse()
                        .withStatus(204)));

        stubFor(get(urlPathMatching(LIMITED_PARTNERSHIP_REGEX))
                .withHeader("X-Request-Id", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(getFilingApiList()))));

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
        WireMock.verify(1, getRequestedFor(urlPathMatching(LIMITED_PARTNERSHIP_REGEX))
                .withQueryParam("resource", matching(".*"))
                .withQueryParam("company_name", matching(".*"))
                .withQueryParam("company_number", matching(".*")));
    }

    @ParameterizedTest
    @MethodSource("filingApiPaths")
    void shouldConsumeTransactionClosedMessagesAndProcessSuccessfullyWithPatch(String expectedFilingPath,
                                                                               String filingRegex) throws Exception {
        // given
        byte[] message = writePayloadToBytes(getTransactionClosedMessage(), transaction_closed.class);
        Transaction transaction = getTransaction();
        Map<String, String> resourceLinks;
        resourceLinks = transaction.getResources().get(TEST_TRANSACTIONS_KEY).getLinks();
        resourceLinks.put("resource", expectedFilingPath);
        transaction.getResources().get(TEST_TRANSACTIONS_KEY).setLinks(resourceLinks);
        ObjectMapper objectMapper = getObjectMapper();

        String transactionString = objectMapper.writeValueAsString(transaction);
        stubFor(get("/private/transactions/987654")
                .willReturn(aResponse()
                        .withStatus(200).withBody(transactionString)));

        stubFor(patch(urlPathEqualTo("/private/transactions/987654"))
                .withQueryParam("force", matching("true"))
                .willReturn(aResponse()
                        .withStatus(204)));

        stubFor(get(urlPathMatching(filingRegex))
                .withHeader("X-Request-Id", matching(".*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(getFilingApiList()))));

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
        WireMock.verify(1, patchRequestedFor(urlEqualTo("/private/transactions/987654?force=true")));
        WireMock.verify(1, getRequestedFor(urlPathMatching(filingRegex))
                .withQueryParam("resource", matching(".*"))
                .withQueryParam("company_name", matching(".*"))
                .withQueryParam("company_number", matching(".*")));
    }
}
