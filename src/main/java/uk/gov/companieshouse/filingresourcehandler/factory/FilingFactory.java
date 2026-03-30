package uk.gov.companieshouse.filingresourcehandler.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;

import java.util.Map;

@Component
public class FilingFactory {

    public Filing getFiling(FilingApi filing, String companyNumber, Map<String, String> links) {
        Filing patchFiling = new Filing();
        patchFiling.setCompanyNumber(companyNumber);
        patchFiling.setDescription(filing.getDescription());
        patchFiling.setDescriptionIdentifier(filing.getDescriptionIdentifier());
        patchFiling.setDescriptionValues(filing.getDescriptionValues());
        patchFiling.setLinks(links);
        patchFiling.setStatus("processing");
        patchFiling.setType(filing.getKind());
        patchFiling.setCost(filing.getCost());
        return patchFiling;
    }
}
