package uk.gov.companieshouse.filingresourcehandler.service;

import accounts.transaction_closed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.apiclient.TransactionsApiClient;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingReceivedFactory;
import uk.gov.companieshouse.filingresourcehandler.factory.ResourceMapFactory;
import uk.gov.companieshouse.filingresourcehandler.kafka.Producer;
import uk.gov.companieshouse.filingresourcehandler.model.FilingProcessingResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilingResourceHandlerServiceTest {

    @Mock
    private TransactionsApiClient transactionsApiClient;
    @Mock
    private FilingResourceProcessorService filingResourceProcessorService;
    @Mock
    private ResourceMapFactory resourceMapFactory;
    @Mock
    private FilingReceivedFactory filingReceivedFactory;
    @Mock
    private Producer producer;

    @InjectMocks
    private FilingResourceHandlerService service;


    @Test
    void processMessagePublishesFilingReceived() {
        // Arrange
        String transactionUrl = "url";
        transaction_closed transactionClosed = mock(transaction_closed.class);
        when(transactionClosed.getTransactionUrl()).thenReturn(transactionUrl);

        Transaction transaction = mock(Transaction.class);
        when(transactionsApiClient.getTransaction(transactionUrl)).thenReturn(Optional.of(transaction));

        Map<String, Resource> resources = new HashMap<>();
        when(resourceMapFactory.createResourceMap(transaction, null, transactionUrl)).thenReturn(resources);

        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(mock(uk.gov.companieshouse.filing.received.Transaction.class));
        Map<String, Filing> filingsToPatch = new HashMap<>();
        FilingProcessingResult result = mock(FilingProcessingResult.class);
        when(result.getItems()).thenReturn(items);
        when(result.getFilingsToPatch()).thenReturn(filingsToPatch);
        when(filingResourceProcessorService.processResources(transaction, resources)).thenReturn(result);

        FilingReceived filingReceived = mock(FilingReceived.class);
        when(filingReceivedFactory.getFilingReceived(items, transaction)).thenReturn(filingReceived);

        // Act
        service.processMessage(transactionClosed);

        // Assert
        verify(producer).publishMessage(filingReceived);
    }

    @Test
    void processMessageThrowsIfNoItems() {
        // Arrange
        String transactionUrl = "url";
        transaction_closed transactionClosed = mock(transaction_closed.class);
        when(transactionClosed.getTransactionUrl()).thenReturn(transactionUrl);

        Transaction transaction = mock(Transaction.class);
        when(transactionsApiClient.getTransaction(transactionUrl)).thenReturn(Optional.of(transaction));

        Map<String, Resource> resources = new HashMap<>();
        when(resourceMapFactory.createResourceMap(transaction, null, transactionUrl)).thenReturn(resources);

        List<uk.gov.companieshouse.filing.received.Transaction> items = Collections.emptyList();
        Map<String, Filing> filingsToPatch = new HashMap<>();
        FilingProcessingResult result = mock(FilingProcessingResult.class);
        when(result.getItems()).thenReturn(items);
        when(result.getFilingsToPatch()).thenReturn(filingsToPatch);
        when(filingResourceProcessorService.processResources(transaction, resources)).thenReturn(result);

        // Act & Assert
        Exception e = assertThrows(RuntimeException.class, () -> service.processMessage(transactionClosed));
        assertThat(e.getMessage()).contains("No resources found in transaction for url");
    }

    @Test
    void processMessageCallsPatchTransactionWhenFilingsToPatchNotEmpty() {
        // Arrange
        String transactionUrl = "url";
        transaction_closed transactionClosed = mock(transaction_closed.class);
        when(transactionClosed.getTransactionUrl()).thenReturn(transactionUrl);

        Transaction transaction = new Transaction(); // Use real Transaction, not mock
        when(transactionsApiClient.getTransaction(transactionUrl)).thenReturn(Optional.of(transaction));

        Map<String, Resource> resources = new HashMap<>();
        when(resourceMapFactory.createResourceMap(transaction, null, transactionUrl)).thenReturn(resources);

        Map<String, Filing> filingsToPatch = new HashMap<>();
        Filing filing = mock(Filing.class);
        filingsToPatch.put("patch-id", filing);
        List<uk.gov.companieshouse.filing.received.Transaction> items = List.of(mock(uk.gov.companieshouse.filing.received.Transaction.class));
        FilingProcessingResult result = mock(FilingProcessingResult.class);
        when(result.getItems()).thenReturn(items);
        when(result.getFilingsToPatch()).thenReturn(filingsToPatch);
        when(filingResourceProcessorService.processResources(transaction, resources)).thenReturn(result);

        FilingReceived filingReceived = mock(FilingReceived.class);
        when(filingReceivedFactory.getFilingReceived(items, transaction)).thenReturn(filingReceived);

        // Act
        service.processMessage(transactionClosed);

        // Assert
        ArgumentCaptor<Transaction> transactionPatchCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionsApiClient).patchTransaction(eq(transactionUrl), transactionPatchCaptor.capture());
    }
}
