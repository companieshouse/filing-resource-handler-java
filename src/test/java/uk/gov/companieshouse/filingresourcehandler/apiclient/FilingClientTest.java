package uk.gov.companieshouse.filingresourcehandler.apiclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilingClientTest {

    @Mock
    private ResponseHandler responseHandler;
    @Mock
    private WebClient webClient;
    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;
    @Mock
    private WebClient.ResponseSpec responseSpec;
    @Mock
    private Mono<FilingApi[]> monoFilingApiArray;
    @InjectMocks
    private FilingClient client;


    @Test
    void getFilingApiReturnsFilingApiArrayOnSuccess() {
        FilingApi[] expected = new FilingApi[]{new FilingApi()};
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(monoFilingApiArray);
        when(monoFilingApiArray.block()).thenReturn(expected);

        Optional<FilingApi[]> result = client.getFilingApi("link", "companyName", "companyNumber");
        assertThat(result).contains(expected);
    }

    @Test
    void getFilingApiThrowsRetryableExceptionOnNullResponse() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(monoFilingApiArray);
        when(monoFilingApiArray.block()).thenReturn(null);
        doThrow(new RetryableException("Unexpected error occurred")).when(responseHandler).handle(any(), any(Exception.class));

        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RetryableException("retryable"));
            assertThrows(RetryableException.class, () -> client.getFilingApi("link", "companyName", "companyNumber"));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()));
        }
    }

    @Test
    void getFilingApiHandlesWebClientResponseException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(monoFilingApiArray);
        when(monoFilingApiArray.block()).thenThrow(WebClientResponseException.class);

        client.getFilingApi("link", "companyName", "companyNumber");
        verify(responseHandler).handle(any(String.class), any(WebClientResponseException.class));
    }

    @Test
    void getFilingApiHandlesGenericException() {
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(any(java.util.function.Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.headers(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(any(org.springframework.core.ParameterizedTypeReference.class))).thenReturn(monoFilingApiArray);
        when(monoFilingApiArray.block()).thenThrow(new RuntimeException("fail"));
        // Use doThrow for void method
        doThrow(new RetryableException("Unexpected error occurred")).when(responseHandler).handle(any(), any(Exception.class));

        assertThatThrownBy(() -> client.getFilingApi("link", "companyName", "companyNumber"))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Unexpected error occurred");
    }
}
