package uk.gov.companieshouse.filingresourcehandler.service;


import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;
import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Service
public class SubmissionIdService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public int findSubmissionIDOffset(Transaction transaction, Map<String, String> transactionMatcher) {
        int largestOffset = 0;
        String transactionID = transaction.getId() != null ? transaction.getId() : "";
        int transactionIDLength = transactionID.length();
        Map<String, Filing> filings = transaction.getFilings();
        if (filings == null || filings.isEmpty()) {
            return largestOffset;
        }
        for (String submissionID : filings.keySet()) {
            if (submissionID == null) {
                LOGGER.info("No submission id found for transaction", DataMapHolder.getLogMap());
                continue;
            }
            try {
                int submissionIDLength = submissionID.length();
                int diff = submissionIDLength - transactionIDLength;
                // Extract the numeric offset from the submissionID
                String offsetStr = submissionID.substring(submissionIDLength + 1 - diff);
                int offset;
                offset = Integer.parseInt(offsetStr);
                updateTransactionMatcher(transactionMatcher, submissionID, filings);
                if (offset > largestOffset) {
                    largestOffset = offset;
                }
            } catch (NumberFormatException | StringIndexOutOfBoundsException ex) {
                String errorMessage = "Invalid offset in submissionID: %s on transactionId %s and error message %s".formatted(submissionID, transaction.getId(), ex.getMessage());
                RetryErrorHandler.logAndThrowRetryableException(errorMessage);
            }
        }
        return largestOffset;
    }

    private void updateTransactionMatcher(Map<String, String> transactionMatcher, String submissionID, Map<String, Filing> filings) {
        Filing filing = filings.getOrDefault(submissionID, new Filing());
        String filingType = filing.getType() != null ? filing.getType() : "";
        Map<String, String> links = filing.getLinks() != null ? filing.getLinks() : new HashMap<>();
        String url = links.getOrDefault("resource", "");
        transactionMatcher.put(format("%s:%s", filingType, url), submissionID);
    }
}
