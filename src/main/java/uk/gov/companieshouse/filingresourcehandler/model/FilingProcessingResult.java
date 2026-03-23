package uk.gov.companieshouse.filingresourcehandler.model;

import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filing.received.Transaction;

import java.util.List;
import java.util.Map;

public class FilingProcessingResult {
    private final Map<String, Filing> filingsToPatch;
    private final List<uk.gov.companieshouse.filing.received.Transaction> items;

    public FilingProcessingResult(Map<String, Filing> filingsToPatch, List<Transaction> items) {
        this.filingsToPatch = filingsToPatch;
        this.items = items;
    }

    public Map<String, Filing> getFilingsToPatch() {
        return filingsToPatch;
    }

    public List<uk.gov.companieshouse.filing.received.Transaction> getItems() {
        return items;
    }
}
