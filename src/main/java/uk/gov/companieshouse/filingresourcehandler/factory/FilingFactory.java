package uk.gov.companieshouse.filingresourcehandler.factory;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;

@Component
public class FilingFactory {

    private static final String STATUS_PROCESSING = "processing";

    public Filing getFiling(FilingApi filing, String companyNumber, Map<String, String> links) {
        if (filing == null) {
            filing = new FilingApi();
        }

        Filing patchFiling = new Filing();
        if (companyNumber != null && !companyNumber.trim().isEmpty()) {
            patchFiling.setCompanyNumber(companyNumber);
        }
        patchFiling.setDescription(StringUtils.defaultString(filing.getDescription()));
        patchFiling.setDescriptionIdentifier(StringUtils.defaultString(filing.getDescriptionIdentifier()));
        patchFiling.setDescriptionValues(filing.getDescriptionValues());
        patchFiling.setLinks(links != null ? links : new HashMap<>());
        patchFiling.setStatus(STATUS_PROCESSING);
        patchFiling.setType(StringUtils.defaultString(filing.getKind()));
        if (filing.getCost() != null && !filing.getCost().trim().isEmpty()) {
            patchFiling.setCost(filing.getCost());
        }
        return patchFiling;
    }
}
