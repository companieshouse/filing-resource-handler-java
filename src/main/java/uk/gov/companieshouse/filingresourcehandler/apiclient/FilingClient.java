package uk.gov.companieshouse.filingresourcehandler.apiclient;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Optional;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Component
public class FilingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String GET_RESOURCE_DATA_URI = "/private%s/filings";
    private final ResponseHandler responseHandler;

    private final WebClient webClient;

    public FilingClient(ResponseHandler responseHandler, WebClient webClient) {
        this.responseHandler = responseHandler;
        this.webClient = webClient;
    }


    public Optional<FilingApi[]> getFilingApi(String link, String companyName, String companyNumber) {
        FilingApi[] response;
        String requestUri = GET_RESOURCE_DATA_URI.formatted(link);
        String requestId = DataMapHolder.getRequestId();
        try {
            MultiValueMap<String, String> queryParams = getQueryParams(link, companyName, companyNumber);
            response = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path(requestUri).queryParams(queryParams).build())
                    .headers(headers -> {
                        if (requestId != null && !requestId.trim().isEmpty()) {
                            headers.add("X-Request-Id", requestId);
                        }
                    })
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<FilingApi[]>() {
                    })
                    .block();
            if (response == null) {
                String errorMessage = "Failed to execute GET getFilingApi with requestUri: %s with null Response".formatted(requestUri);
                RetryErrorHandler.logAndThrowRetryableException(errorMessage);
            }
            LOGGER.info("Successfully executed GET Filings", DataMapHolder.getLogMap());
            return Optional.of(response);

        } catch (WebClientResponseException ex) {
            String webClientExceptionMessage = "Failed to execute GET getFilingApi with requestUri: %s with status code: %s and stacktrace %s".formatted(requestUri, ex.getStatusCode(), ex.getStackTrace());
            responseHandler.handle(webClientExceptionMessage, ex);
        } catch (Exception ex) {
            String defaultErrorMessage = "Unexpected error occurred during GET getFilingApi with requestUri: %s with message: %s and stacktrace %s".formatted(requestUri, ex.getMessage(), ex.getStackTrace());
            LOGGER.error(defaultErrorMessage, DataMapHolder.getLogMap());
            responseHandler.handle(defaultErrorMessage, ex);
        }
        return Optional.empty();
    }

    private static MultiValueMap<String, String> getQueryParams(String link, String companyName, String companyNumber) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("resource", link);
        queryParams.add("company_name", companyName);
        if (!companyNumber.isBlank())
            queryParams.add("company_number", companyNumber);
        return queryParams;
    }
}
