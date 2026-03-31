package uk.gov.companieshouse.filingresourcehandler.apiclient;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Arrays;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Component
public class ResponseHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String API_INFO_MESSAGE = "GET call to API failed, status code: %d. %s";
    private static final String API_ERROR_MESSAGE = "GET call to API failed, status code: %d";
    private static final String URI_VALIDATION_EXCEPTION_MESSAGE = "Invalid URI";

    public void handle(ApiErrorResponseException ex) {
        final int statusCode = ex.getStatusCode();
        final HttpStatus httpStatus = HttpStatus.valueOf(ex.getStatusCode());

        String errorMsg = API_ERROR_MESSAGE.formatted(statusCode);
        if (HttpStatus.CONFLICT.equals(httpStatus) || HttpStatus.BAD_REQUEST.equals(httpStatus)) {
            LOGGER.error(errorMsg, ex, DataMapHolder.getLogMap());
            throw new NonRetryableException(errorMsg, ex);
        } else {
            errorMsg = API_INFO_MESSAGE.formatted(statusCode, Arrays.toString(ex.getStackTrace()));
            LOGGER.error(errorMsg, ex, DataMapHolder.getLogMap());
            throw new RetryableException(errorMsg, ex);
        }
    }

    public void handle(URIValidationException ex) {
        LOGGER.error(URI_VALIDATION_EXCEPTION_MESSAGE, ex, DataMapHolder.getLogMap());
        throw new NonRetryableException(URI_VALIDATION_EXCEPTION_MESSAGE, ex);
    }

    public void handle(String errorMessage, WebClientResponseException ex) {
        final int statusCode = ex.getStatusCode().value();
        if (HttpStatus.BAD_REQUEST.value() == statusCode || HttpStatus.CONFLICT.value() == statusCode) {
            LOGGER.error(errorMessage, DataMapHolder.getLogMap());
            throw new NonRetryableException(errorMessage, ex);
        } else {
            LOGGER.error(
                    errorMessage,
                    DataMapHolder.getLogMap());
            throw new RetryableException(errorMessage, ex);
        }
    }


    public void handle(int statusCode) {
        HttpResponseException.Builder exBuilder = new HttpResponseException.Builder(statusCode, "status code: %d".formatted(statusCode), new HttpHeaders());
        handle(new ApiErrorResponseException(exBuilder));
    }

    public void handle(String errorMessage, Exception ex) {
        LOGGER.error(
                errorMessage,
                DataMapHolder.getLogMap());
        throw new RetryableException(errorMessage, ex);
    }
}
