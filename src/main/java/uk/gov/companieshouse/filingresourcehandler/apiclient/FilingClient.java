package uk.gov.companieshouse.filingresourcehandler.apiclient;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class FilingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final String GET_RESOURCE_DATA_URI = "/private%s/filings";
    private final ResponseHandler responseHandler;

    private final RestClient restClient;

    public FilingClient(ResponseHandler responseHandler, RestClient restClient) {
        this.responseHandler = responseHandler;
        this.restClient = restClient;
    }


    public Optional<FilingApi[]> getFilingApi(String link, String companyName, String companyNumber) {
        String requestUri = GET_RESOURCE_DATA_URI.formatted(link);
        String requestId = DataMapHolder.getRequestId();
        MultiValueMap<String, String> queryParams = getQueryParams(link, companyName, companyNumber);
        try {
            FilingApi[] response = restClient.get()
                    .uri(uriBuilder -> uriBuilder.path(requestUri).queryParams(queryParams).build())
                    .headers(headers -> {
                        if (requestId != null && !requestId.trim().isEmpty()) {
                            headers.add("X-Request-Id", requestId);
                        }
                    })
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            if (response == null) {
                String errorMessage = "Failed to execute GET getFilingApi with requestUri: %s with null Response".formatted(
                        requestUri);
                RetryErrorHandler.logAndThrowRetryableException(errorMessage);
            }
            LOGGER.info("Successfully executed GET Filings", DataMapHolder.getLogMap());
            return Optional.of(response);
        } catch (RestClientResponseException ex) {
            String restClientExceptionMessage = "Failed to execute GET getFilingApi with requestUri: %s with status code: %s and stacktrace %s".formatted(
                    requestUri, ex.getStatusCode(), ex.getStackTrace());
            responseHandler.handle(restClientExceptionMessage, ex);
        }
        return Optional.empty();
    }

    private static MultiValueMap<String, String> getQueryParams(String link, String companyName, String companyNumber) {
        MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        queryParams.add("resource", link);
        queryParams.add("company_name", companyName);
        if (StringUtils.isNotBlank(companyNumber)) {
            queryParams.add("company_number", companyNumber);
        }
        return queryParams;
    }
}
