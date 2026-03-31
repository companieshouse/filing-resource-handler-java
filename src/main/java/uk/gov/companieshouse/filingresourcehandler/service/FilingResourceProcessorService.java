package uk.gov.companieshouse.filingresourcehandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.apiclient.FilingClient;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.model.FilingProcessingResult;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Service
public class FilingResourceProcessorService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private final FilingClient filingClient;
    private final SubmissionIdService submissionIdService;
    private final FilingPatchService filingPatchService;
    private final ItemService itemService;

    public FilingResourceProcessorService(FilingClient filingClient, SubmissionIdService submissionIdService, FilingPatchService filingPatchService, ItemService itemService) {
        this.filingClient = filingClient;
        this.itemService = itemService;
        this.submissionIdService = submissionIdService;
        this.filingPatchService = filingPatchService;
    }

    public FilingProcessingResult processResources(Transaction transaction, Map<String, Resource> resources) {
        Map<String, Filing> transactionsFilingMap = new HashMap<>();
        List<uk.gov.companieshouse.filing.received.Transaction> items = new ArrayList<>();
        Map<String, String> transactionMatcher = new HashMap<>();
        int offset = submissionIdService.findSubmissionIDOffset(transaction, transactionMatcher);
        String companyNumber = transaction.getCompanyNumber();
        for (Resource resource : resources.values()) {
            String link = resource.getLinks().get("resource");
            LOGGER.info("Found resource for current transaction", DataMapHolder.getLogMap());
            FilingApi[] filings = filingClient.getFilingApi(link, transaction.getCompanyName(), transaction.getCompanyNumber()).orElseThrow(() -> {
                String errorMessage = "Empty Filings response for transactionId %s".formatted(transaction.getId());
                LOGGER.error(errorMessage, DataMapHolder.getLogMap());
                return new RetryableException(errorMessage);
            });
            for (FilingApi filing : filings) {
                String submissionId = transactionMatcher.get("%s:%s".formatted(filing.getKind(), link));
                if (submissionId == null) {
                    offset++;
                    submissionId = "%s-%d".formatted(transaction.getId(), offset);
                    LOGGER.info("Add filing to patch request with submissionId %s".formatted(submissionId), DataMapHolder.getLogMap());
                    filingPatchService.addFilingToPatch(transactionsFilingMap, filing, submissionId, link, companyNumber);
                }
                itemService.addItems(filing, submissionId, items);
            }
        }

        return new FilingProcessingResult(transactionsFilingMap, items);
    }
    
}
