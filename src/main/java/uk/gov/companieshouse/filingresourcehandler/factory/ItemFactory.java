package uk.gov.companieshouse.filingresourcehandler.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;

@Component
public class ItemFactory {
    public uk.gov.companieshouse.filing.received.Transaction getItem(FilingApi filing, String submissionId, String filingDataJson) {
        uk.gov.companieshouse.filing.received.Transaction item = new uk.gov.companieshouse.filing.received.Transaction();
        item.setData(filingDataJson);
        item.setKind(filing.getKind());
        item.setSubmissionId(submissionId);
        item.setSubmissionLanguage("en");
        return item;
    }
}
