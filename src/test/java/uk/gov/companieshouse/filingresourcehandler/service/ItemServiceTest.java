package uk.gov.companieshouse.filingresourcehandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filing.received.Transaction;
import uk.gov.companieshouse.filingresourcehandler.factory.ItemFactory;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {
    @Mock
    private ItemFactory itemFactory;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ItemService itemService;

    @Test
    void addItemsAddsItemToList() throws Exception {
        FilingApi filingApi = new FilingApi();
        filingApi.setData(new java.util.HashMap<>());
        String submissionId = "sub-id";
        List<Transaction> items = new ArrayList<>();
        String json = "{}";
        Transaction transaction = new Transaction();
        when(objectMapper.writeValueAsString(any())).thenReturn(json);
        when(itemFactory.getItem(filingApi, submissionId, json)).thenReturn(transaction);

        itemService.addItems(filingApi, submissionId, items);

        assertThat(items).containsExactly(transaction);
        verify(objectMapper).writeValueAsString(filingApi.getData());
        verify(itemFactory).getItem(filingApi, submissionId, json);
    }

    @Test
    void addItemsThrowsRetryableExceptionOnJsonError() throws Exception {
        FilingApi filingApi = new FilingApi();
        filingApi.setData(new java.util.HashMap<>());
        String submissionId = "sub-id";
        List<Transaction> items = new ArrayList<>();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("fail") {
        });
        // Spy static method
        try (var mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(any())).thenThrow(new RuntimeException("retryable"));
            assertThrows(RuntimeException.class, () -> itemService.addItems(filingApi, submissionId, items));
            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(contains(submissionId)));
        }
    }
}
