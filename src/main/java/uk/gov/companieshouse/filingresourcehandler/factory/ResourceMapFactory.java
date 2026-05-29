package uk.gov.companieshouse.filingresourcehandler.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResourceMapFactory {

    private static final String DEFAULT = "default";
    private static final String ERROR_NO_MATCH =
            "filing mode %s is set for a master resource but no resource on transaction URL %s has that kind";
    private static final String ERROR_MULTIPLE_MATCHES =
            "filing mode %s is set for a master resource but more than one resource on the transaction URL %s has that kind";

    public Map<String, Resource> createResourceMap(Transaction transaction,
                                                   String transactionFilingMode,
                                                   String transactionUrl) {
        Map<String, Resource> resources = transaction.getResources();

        boolean isCustomMode = transactionFilingMode != null
                && !transactionFilingMode.isBlank()
                && !DEFAULT.equals(transactionFilingMode);

        if (!isCustomMode || resources == null) {
            return resources != null ? resources : new HashMap<>();
        }

        Map<String, Resource> filtered = getStringResourceMap(transactionFilingMode, resources);

        if (filtered.isEmpty()) {
            RetryErrorHandler.logAndThrowRetryableException(
                    ERROR_NO_MATCH.formatted(transactionFilingMode, transactionUrl));
        } else if (filtered.size() > 1) {
            RetryErrorHandler.logAndThrowRetryableException(
                    ERROR_MULTIPLE_MATCHES.formatted(transactionFilingMode, transactionUrl));
        }
        return filtered;
    }

    private static Map<String, Resource> getStringResourceMap(String transactionFilingMode, Map<String, Resource> resources) {
        Map<String, Resource> filtered = new HashMap<>();
        for (Map.Entry<String, Resource> entry : resources.entrySet()) {
            Resource resource = entry.getValue();
            String kind = resource != null ? resource.getKind() : null;
            if (kind == null) {
                continue;
            }
            int hashIdx = kind.indexOf('#');
            String mainKind = hashIdx == -1 ? kind : kind.substring(0, hashIdx);
            if (mainKind.equals(transactionFilingMode)) {
                filtered.put(entry.getKey(), resource);
            }
        }
        return filtered;
    }
}