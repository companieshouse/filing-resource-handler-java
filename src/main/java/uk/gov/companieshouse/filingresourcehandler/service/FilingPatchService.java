package uk.gov.companieshouse.filingresourcehandler.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Map;
import java.util.Optional;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Service
public class FilingPatchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    private static final String INSOLVENCY = "insolvency";
    private static final String COMPANY_NUMBER = "company_number";
    private static final String RESOURCE = "resource";

    private final FilingFactory filingFactory;

    public FilingPatchService(FilingFactory filingFactory) {
        this.filingFactory = filingFactory;
    }


    public void addFilingToPatch(Map<String, Filing> transactionsFilingMap, FilingApi filing, String submissionId, String link, String companyNumber) {
        String filingKind = filing.getKind();
        if (filingKind != null && filingKind.toLowerCase().contains(INSOLVENCY)) {
            companyNumber = Optional.ofNullable(filing.getData())
                    .map(data -> data.get(COMPANY_NUMBER))
                    .map(Object::toString)
                    .filter(value -> !value.isBlank())
                    .orElseThrow(() -> {
                        String message = "Missing company_number or filing data for insolvency filing with submissionId : %s".formatted(submissionId);
                        LOGGER.error(message, DataMapHolder.getLogMap());
                        return new NonRetryableException(message);
                    });
        }
        Map<String, String> links = Map.of(RESOURCE, link);
        Filing patchFiling = filingFactory.getFiling(filing, companyNumber, links);
        transactionsFilingMap.put(submissionId, patchFiling);
    }
}
