package uk.gov.companieshouse.filingresourcehandler.service;


import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.Map;

import static java.lang.String.format;

@Service
public class SubmissionIdService {

    public int findSubmissionIDOffset(Transaction transaction, Map<String, String> transactionMatcher) {
        int largestOffset = 0;
        int transactionIDLength = transaction.getId().length();
        for (String submissionID : transaction.getFilings().keySet()) {
            int submissionIDLength = submissionID.length();
            int diff = submissionIDLength - transactionIDLength;
            // Extract the numeric offset from the submissionID
            String offsetStr = submissionID.substring(submissionIDLength + 1 - diff);
            int offset = 0;
            try {
                offset = Integer.parseInt(offsetStr);
            } catch (NumberFormatException ex) {
                String errorMessage = "Invalid offset in submissionID: %s on transactionId %s and error message %s".formatted(submissionID, transaction.getId(), ex.getMessage());
                RetryErrorHandler.logAndThrowRetryableException(errorMessage);
            }
            Filing filing = transaction.getFilings().get(submissionID);
            String filingType = filing.getType();
            String url = filing.getLinks().get("resource");
            transactionMatcher.put(format("%s:%s", filingType, url), submissionID);
            if (offset > largestOffset) {
                largestOffset = offset;
            }
        }
        return largestOffset;
    }
}
