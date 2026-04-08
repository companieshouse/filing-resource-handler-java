package uk.gov.companieshouse.filingresourcehandler.apiclient;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.api.request.QueryParam;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;


@Component
public class TransactionsApiClient {

    private static final String TRANSACTION_URI = "/private/%s";
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private static final List<QueryParam> FORCE_QUERY_PARAM = List.of(new QueryParam("force", "true"));
    private static final String GET_API_CALL = "GET call to transaction";
    private static final String PATCH_API_CALL = "Patch call to transaction";

    private final Supplier<InternalApiClient> internalApiClientFactory;
    private final ResponseHandler responseHandler;

    public TransactionsApiClient(Supplier<InternalApiClient> internalApiClientFactory, ResponseHandler responseHandler) {
        this.internalApiClientFactory = internalApiClientFactory;
        this.responseHandler = responseHandler;
    }

    public Optional<Transaction> getTransaction(String transactionUrl) {
        InternalApiClient internalApiClient = internalApiClientFactory.get();
        internalApiClient.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        ApiResponse<Transaction> response;
        try {
            String requestUri = TRANSACTION_URI.formatted(transactionUrl);
            response = internalApiClient.privateTransaction().get(requestUri).execute();
            if (response.getStatusCode() != HttpStatus.OK.value()) {
                LOGGER.error("Failed to execute get transactions transactionId: %s with status code: %d".formatted(requestUri, response.getStatusCode()), DataMapHolder.getLogMap());
                responseHandler.handle(GET_API_CALL, response.getStatusCode());
            } else {
                LOGGER.info("Successfully executed get transactions", DataMapHolder.getLogMap());
            }
            return Optional.ofNullable(response.getData());
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(GET_API_CALL, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(GET_API_CALL, ex);
        }
        return Optional.empty();
    }

    public void patchTransaction(String patchRequestUri, Transaction transaction) {
        InternalApiClient client = internalApiClientFactory.get();
        client.getHttpClient().setRequestId(DataMapHolder.getRequestId());
        patchRequestUri = TRANSACTION_URI.formatted(patchRequestUri);
        DataMapHolder.get().uri(patchRequestUri);
        LOGGER.info("Calling PATCH transaction for patchBody %s".formatted(transaction), DataMapHolder.getLogMap());

        try {
            ApiResponse<Void> response = client.privateTransaction()
                    .patch(patchRequestUri, transaction)
                    .queryParams(FORCE_QUERY_PARAM)
                    .execute();
            if (response.getStatusCode() == HttpStatus.NO_CONTENT.value()) {
                LOGGER.info("PATCH transaction succeeded", DataMapHolder.getLogMap());
            } else {
                responseHandler.handle(PATCH_API_CALL, response.getStatusCode());
            }
        } catch (ApiErrorResponseException ex) {
            responseHandler.handle(PATCH_API_CALL, ex);
        } catch (URIValidationException ex) {
            responseHandler.handle(PATCH_API_CALL, ex);
        }
    }

}