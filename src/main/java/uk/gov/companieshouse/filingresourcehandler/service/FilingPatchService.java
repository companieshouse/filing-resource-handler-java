package uk.gov.companieshouse.filingresourcehandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class FilingPatchService {

    private final FilingFactory filingFactory;

    public FilingPatchService(FilingFactory filingFactory) {
        this.filingFactory = filingFactory;
    }


    public void addFilingToPatch(Map<String, Filing> transactionsFilingMap, FilingApi filing, String submissionId, String link, String companyNumber) {
        if (filing.getKind().toLowerCase().contains("insolvency")) {
            companyNumber = (String) filing.getData().get("company_number");
        }
        HashMap<String, String> links = new HashMap<>();
        links.put("Resource", link);
        Filing patchFiling = filingFactory.getFiling(filing, companyNumber, links);
        transactionsFilingMap.put(submissionId, patchFiling);
    }
}
