package uk.gov.companieshouse.filingresourcehandler.apiclient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

@ExtendWith(MockitoExtension.class)
class FilingClientTest {

    private static final String FILING_URI = "http://localhost/private/transactions/123/filings?resource=/transactions/123&company_name=companyName&company_number=00006400";
    private static final String TRANSACTIONS_123 = "/transactions/123";
    private static final String COMPANY_NAME = "companyName";
    private static final String COMPANY_NUMBER = "00006400";

    @Mock
    private ResponseHandler responseHandler;
    private MockRestServiceServer server;
    private FilingClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new FilingClient(responseHandler, builder.build());
    }

    @Test
    void getFilingApiReturnsFilingApiArrayOnSuccess() {
        server.expect(requestTo(FILING_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("[{\"kind\":\"test-kind\"}]", MediaType.APPLICATION_JSON));

        Optional<FilingApi[]> result = client.getFilingApi(TRANSACTIONS_123, COMPANY_NAME, COMPANY_NUMBER);

        assertThat(result).isPresent();
        assertThat(result.get()).hasSize(1);
        assertThat(result.get()[0].getKind()).isEqualTo("test-kind");
        server.verify();
    }

    @Test
    void getFilingApiThrowsRetryableExceptionOnNullResponse() {
        server.expect(requestTo(FILING_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withNoContent());

        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RetryableException("retryable"));

            assertThrows(RetryableException.class,
                    () -> client.getFilingApi(TRANSACTIONS_123, COMPANY_NAME, COMPANY_NUMBER));

            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()));
        }

        server.verify();
    }

    @Test
    void getFilingApiHandlesRestClientResponseException() {
        doThrow(new RetryableException("retryable from handler"))
                .when(responseHandler)
                .handle(anyString(), any(RestClientResponseException.class));

        server.expect(requestTo(FILING_URI))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getFilingApi(TRANSACTIONS_123, COMPANY_NAME, COMPANY_NUMBER))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("retryable from handler");

        verify(responseHandler).handle(anyString(), any(RestClientResponseException.class));
        server.verify();
    }
}
