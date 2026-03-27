package uk.gov.companieshouse.filingresourcehandler.service;

import accounts.transaction_closed;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filingresourcehandler.apiclient.TransactionsApiClient;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingReceivedFactory;
import uk.gov.companieshouse.filingresourcehandler.factory.ResourceMapFactory;
import uk.gov.companieshouse.filingresourcehandler.kafka.Producer;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.model.FilingProcessingResult;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Map;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Service
public class FilingResourceHandlerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);
    private final TransactionsApiClient transactionsApiClient;
    private final FilingResourceProcessorService filingResourceProcessorService;
    private final ResourceMapFactory resourceMapFactory;
    private final FilingReceivedFactory filingReceivedFactory;
    private final Producer producer;

    public FilingResourceHandlerService(TransactionsApiClient transactionsApiClient,
                                        FilingResourceProcessorService filingResourceProcessorService,
                                        ResourceMapFactory resourceMapFactory, FilingReceivedFactory filingReceivedFactory, Producer producer) {
        this.transactionsApiClient = transactionsApiClient;
        this.filingResourceProcessorService = filingResourceProcessorService;
        this.resourceMapFactory = resourceMapFactory;
        this.filingReceivedFactory = filingReceivedFactory;
        this.producer = producer;
    }


    public void processMessage(transaction_closed transactionClosed) {
        String transactionUrl = transactionClosed.getTransactionUrl();
        LOGGER.info("Processing Transaction Closed message with url: %s".formatted(transactionUrl), DataMapHolder.getLogMap());
        Transaction transaction = transactionsApiClient.getTransaction(transactionUrl).
                orElseThrow(() -> {
                    String errorMessage = "Empty Transaction response for : %s".formatted(transactionUrl);
                    LOGGER.error(errorMessage, DataMapHolder.getLogMap());
                    return new RetryableException(errorMessage);
                });

        String transactionFilingMode = transaction.getFilingMode();
        Map<String, Resource> resources = resourceMapFactory.createResourceMap(transaction, transactionFilingMode, transactionUrl);
        List<uk.gov.companieshouse.filing.received.Transaction> items;
        FilingProcessingResult filingProcessingResult = filingResourceProcessorService.processResources(transaction, resources);
        items = filingProcessingResult.getItems();
        Map<String, Filing> filingsToPatch = filingProcessingResult.getFilingsToPatch();
        if (!filingsToPatch.isEmpty()) {
            Transaction transactionPatch = new Transaction();
            transaction.setFilings(filingsToPatch);
            transactionsApiClient.patchTransaction(transactionUrl, transactionPatch);
        }
        if (items.isEmpty()) {
            String errorMessage = "No resources found in transaction for url %s".formatted(transactionUrl);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        }
        FilingReceived filingReceived = filingReceivedFactory.getFilingReceived(items, transaction);
        producer.publishMessage(filingReceived);
    }


}
