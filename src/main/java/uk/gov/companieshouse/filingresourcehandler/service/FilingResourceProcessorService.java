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

    public FilingResourceProcessorService(FilingClient filingClient,
                                          SubmissionIdService submissionIdService,
                                          FilingPatchService filingPatchService,
                                          ItemService itemService) {
        this.filingClient = filingClient;
        this.submissionIdService = submissionIdService;
        this.filingPatchService = filingPatchService;
        this.itemService = itemService;
    }

    public FilingProcessingResult processResources(Transaction transaction, Map<String, Resource> resources) {
        Map<String, Filing> filingsToPatch = new HashMap<>();
        List<uk.gov.companieshouse.filing.received.Transaction> items = new ArrayList<>();
        Map<String, String> matcher = new HashMap<>();
        int offset = submissionIdService.findSubmissionIDOffset(transaction, matcher);

        if (resources == null) {
            return new FilingProcessingResult(filingsToPatch, items);
        }

        for (Resource resource : resources.values()) {
            if (resource == null) {
                continue;
            }
            offset = processResource(resource, transaction, matcher, filingsToPatch, items, offset);
        }

        return new FilingProcessingResult(filingsToPatch, items);
    }

    private int processResource(Resource resource,
                                Transaction transaction,
                                Map<String, String> matcher,
                                Map<String, Filing> filingsToPatch,
                                List<uk.gov.companieshouse.filing.received.Transaction> items,
                                int offset) {
        String link = resourceLink(resource);
        LOGGER.info("Found resource of type %s".formatted(orEmpty(resource.getKind())), DataMapHolder.getLogMap());

        FilingApi[] filings = filingClient
                .getFilingApi(link, transaction.getCompanyName(), transaction.getCompanyNumber())
                .orElseThrow(() -> emptyFilingsError(transaction.getId()));

        for (FilingApi filing : filings) {
            if (filing == null || filing.getKind() == null) {
                continue;
            }
            offset = processFiling(filing, link, transaction, matcher, filingsToPatch, items, offset);
        }
        return offset;
    }

    private int processFiling(FilingApi filing,
                              String link,
                              Transaction transaction,
                              Map<String, String> matcher,
                              Map<String, Filing> filingsToPatch,
                              List<uk.gov.companieshouse.filing.received.Transaction> items,
                              int offset) {
        String key = "%s:%s".formatted(filing.getKind(), link);
        String submissionId = matcher.get(key);

        if (submissionId == null) {
            offset++;
            submissionId = "%s-%d".formatted(transaction.getId(), offset);
            LOGGER.info("Add filing to patch request with submissionId %s".formatted(submissionId),
                    DataMapHolder.getLogMap());
            filingPatchService.addFilingToPatch(
                    filingsToPatch, filing, submissionId, link, transaction.getCompanyNumber());
        }
        itemService.addItems(filing, submissionId, items);
        return offset;
    }

    private static String resourceLink(Resource resource) {
        Map<String, String> links = resource.getLinks();
        return links != null ? links.getOrDefault("resource", "") : "";
    }

    private static String orEmpty(String value) {
        return value != null ? value : "";
    }

    private RetryableException emptyFilingsError(String transactionId) {
        String message = "Empty Filings response for transactionId %s".formatted(transactionId);
        LOGGER.error(message, DataMapHolder.getLogMap());
        return new RetryableException(message);
    }
}