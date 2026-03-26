package uk.gov.companieshouse.filingresourcehandler.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filing.received.Transaction;

@Component
public class ItemFactory {
    public Transaction getItem(FilingApi filing, String submissionId, String filingDataJson) {
        Transaction item = new Transaction();
        item.setData(filingDataJson);
        item.setKind(filing.getKind());
        item.setSubmissionId(submissionId);
        item.setSubmissionLanguage("en");
        return item;
    }
}
