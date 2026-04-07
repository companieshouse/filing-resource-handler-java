package uk.gov.companieshouse.filingresourcehandler.apiclient;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.catchThrowable;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.GET_API_CALL;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.PATCH_API_CALL;

@ExtendWith(MockitoExtension.class)
class ResponseHandlerTest {


    @Mock
    WebClientResponseException webClientResponseException;

    @Mock
    ApiErrorResponseException apiErrorResponseException;

    @InjectMocks
    ResponseHandler handler;

    @Test
    void handleApiErrorResponseExceptionThrowsNonRetryableForConflict() {
        when(apiErrorResponseException.getStatusCode()).thenReturn(HttpStatus.CONFLICT.value());
        Throwable thrown = catchThrowable(() -> handler.handle(GET_API_CALL, apiErrorResponseException));
        assertThat(thrown)
                .isInstanceOf(NonRetryableException.class)
                .hasMessageContaining("GET call to transaction API failed, status code");
    }

    @Test
    void handleApiErrorResponseExceptionThrowsNonRetryableForBadRequest() {
        when(apiErrorResponseException.getStatusCode()).thenReturn(HttpStatus.BAD_REQUEST.value());
        assertThatThrownBy(() -> handler.handle(GET_API_CALL, apiErrorResponseException))
                .isInstanceOf(NonRetryableException.class)
                .hasMessageContaining("GET call to transaction API failed, status code");
    }

    @Test
    void handleApiErrorResponseExceptionThrowsRetryableForOtherStatus() {
        when(apiErrorResponseException.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThatThrownBy(() -> handler.handle(PATCH_API_CALL, apiErrorResponseException))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Patch call to transaction API failed, status code");
    }

    @Test
    void handleURIValidationExceptionThrowsNonRetryable() {
        URIValidationException ex = new URIValidationException("bad uri");
        assertThatThrownBy(() -> handler.handle(GET_API_CALL, ex))
                .isInstanceOf(NonRetryableException.class)
                .hasMessageContaining("Invalid URI");
    }

    @Test
    void handleWebClientResponseExceptionThrowsNonRetryableForBadRequestOrConflict() {
        WebClientResponseException ex = WebClientResponseException.create(
                400,
                "Bad Request",
                HttpHeaders.EMPTY,
                null,
                null
        );
        assertThatThrownBy(() -> handler.handle("error", ex))
                .isInstanceOf(NonRetryableException.class);
    }

    @Test
    void handleWebClientResponseExceptionThrowsRetryableForOtherStatus() {
        when(webClientResponseException.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThatThrownBy(() -> handler.handle("error", webClientResponseException))
                .isInstanceOf(RetryableException.class);
    }

    @Test
    void handleIntStatusCodeDelegatesToApiErrorResponseException() {
        ResponseHandler spyHandler = spy(handler);
        doThrow(new NonRetryableException("fail")).when(spyHandler).handle(any(), any(ApiErrorResponseException.class));
        int statusCode = HttpStatus.CONFLICT.value();
        assertThatThrownBy(() -> spyHandler.handle(GET_API_CALL, statusCode))
                .isInstanceOf(NonRetryableException.class);
    }
}


