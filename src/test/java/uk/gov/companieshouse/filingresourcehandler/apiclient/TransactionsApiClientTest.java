package uk.gov.companieshouse.filingresourcehandler.apiclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.handler.privatetransaction.PrivateTransactionResourceHandler;
import uk.gov.companieshouse.api.handler.privatetransaction.request.PrivateTransactionGet;
import uk.gov.companieshouse.api.handler.privatetransaction.request.PrivateTransactionPatch;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.transaction.Transaction;

import java.util.function.Supplier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.GET_API_CALL;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.PATCH_API_CALL;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getTransaction;

@ExtendWith(MockitoExtension.class)
class TransactionsApiClientTest {

    @Mock
    private Supplier<InternalApiClient> internalApiClientFactory;
    @Mock
    private InternalApiClient internalApiClient;
    @Mock
    private PrivateTransactionResourceHandler privateTransactionResourceHandler;
    @Mock
    private PrivateTransactionPatch privateTransactionPatch;
    @Mock
    private HttpClient httpClient;
    @Mock
    private ResponseHandler responseHandler;
    @Mock
    private PrivateTransactionGet privateTransactionGet;
    @InjectMocks
    private TransactionsApiClient transactionsApiClient;

    @BeforeEach
    void setUp() {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.privateTransaction()).thenReturn(privateTransactionResourceHandler);
    }

    @Test
    void getTransactionReturnsTransactionOnSuccess() throws ApiErrorResponseException, URIValidationException {
        ApiResponse<Transaction> apiResponse = new ApiResponse<>(HttpStatus.OK.value(), null, getTransaction());
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionGet.execute()).thenReturn(apiResponse);

        // Act
        var result = transactionsApiClient.getTransaction("txn-id");

        // Assert
        verify(privateTransactionResourceHandler).get(any());
        verify(privateTransactionGet).execute();
        assert (result.isPresent());
        assert (result.get().getId().equals(getTransaction().getId()));
    }

    @Test
    void getTransactionHandlesErrorStatus() throws ApiErrorResponseException, URIValidationException {
        ApiResponse<Transaction> apiResponse = new ApiResponse<>(400, null, null);
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionGet.execute()).thenReturn(apiResponse);
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);

        var result = transactionsApiClient.getTransaction("txn-id");
        verify(privateTransactionResourceHandler).get(any());
        verify(privateTransactionGet).execute();
        verify(responseHandler).handle(GET_API_CALL, 400);
        assert (result.isEmpty());
    }

    @Test
    void getTransactionHandlesApiErrorResponseException() throws ApiErrorResponseException, URIValidationException {
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionGet.execute()).thenThrow(ApiErrorResponseException.class);

        transactionsApiClient.getTransaction("txn-id");
        verify(privateTransactionResourceHandler).get(any());
        verify(privateTransactionGet).execute();
        verify(responseHandler).handle(anyString(), any(ApiErrorResponseException.class));
    }

    @Test
    void getTransactionHandlesURIValidationException() throws ApiErrorResponseException, URIValidationException {
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionResourceHandler.get(any())).thenReturn(privateTransactionGet);
        when(privateTransactionGet.execute()).thenThrow(new URIValidationException("bad uri"));

        transactionsApiClient.getTransaction("txn-id");
        verify(privateTransactionResourceHandler).get(any());
        verify(privateTransactionGet).execute();
        verify(responseHandler).handle(anyString(), any(URIValidationException.class));
    }

    @Test
    void patchTransactionHandlesSuccess() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateTransaction()).thenReturn(privateTransactionResourceHandler);
        when(privateTransactionResourceHandler.patch(any(), any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.queryParams(any())).thenReturn(privateTransactionPatch);
        ApiResponse<Void> patchResponse = mock(ApiResponse.class);
        when(privateTransactionPatch.execute()).thenReturn(patchResponse);
        when(patchResponse.getStatusCode()).thenReturn(204);
        Transaction transaction = new Transaction();
        transactionsApiClient.patchTransaction("patch-uri", transaction);
        verify(patchResponse).getStatusCode();
    }

    @Test
    void patchTransactionHandlesError() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateTransaction()).thenReturn(privateTransactionResourceHandler);
        when(privateTransactionResourceHandler.patch(any(), any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.queryParams(any())).thenReturn(privateTransactionPatch);
        ApiResponse<Void> patchResponse = mock(ApiResponse.class);
        when(privateTransactionPatch.execute()).thenReturn(patchResponse);
        when(patchResponse.getStatusCode()).thenReturn(400);
        Transaction transaction = new Transaction();
        transactionsApiClient.patchTransaction("patch-uri", transaction);
        verify(responseHandler).handle(PATCH_API_CALL, 400);
    }


    @Test
    void patchTransactionHandlesApiErrorResponseException() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateTransaction()).thenReturn(privateTransactionResourceHandler);
        when(privateTransactionResourceHandler.patch(any(), any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.queryParams(any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.execute()).thenThrow(ApiErrorResponseException.class);
        Transaction transaction = new Transaction();

        transactionsApiClient.patchTransaction("patch-uri", transaction);
        verify(responseHandler).handle(anyString(), any(ApiErrorResponseException.class));
    }

    @Test
    void patchTransactionHandlesURIValidationException() throws ApiErrorResponseException, URIValidationException {
        when(internalApiClientFactory.get()).thenReturn(internalApiClient);
        when(internalApiClient.privateTransaction()).thenReturn(privateTransactionResourceHandler);
        when(privateTransactionResourceHandler.patch(any(), any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.queryParams(any())).thenReturn(privateTransactionPatch);
        when(privateTransactionPatch.execute()).thenThrow(new URIValidationException("bad uri"));
        Transaction transaction = new Transaction();

        transactionsApiClient.patchTransaction("patch-uri", transaction);
        verify(responseHandler).handle(anyString(), any(URIValidationException.class));
    }
}
