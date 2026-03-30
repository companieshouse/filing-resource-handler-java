package uk.gov.companieshouse.filingresourcehandler.factory;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.HashMap;
import java.util.Map;

@Component
public class ResourceMapFactory {

    public Map<String, Resource> createResourceMap(Transaction transaction, String transactionFilingMode, String transactionUrl) {
        Map<String, Resource> resources = transaction.getResources();
        if (transaction.getFilingMode() != null && !transaction.getFilingMode().isBlank() && !transaction.getFilingMode().equals("default")) {
            resources = new HashMap<>();
            populateResourceMap(transaction, transactionFilingMode, resources, transactionUrl);
        }
        return resources;
    }

    private void populateResourceMap(Transaction transaction, String transactionFilingMode, Map<String, Resource> resources, String transactionUrl) {
        for (Map.Entry<String, Resource> resourceEntry : transaction.getResources().entrySet()) {
            String kind = resourceEntry.getValue().getKind();
            if (kind == null) {
                continue;
            }
            String mainKind = kind.indexOf('#') == -1 ? kind : kind.substring(0, kind.indexOf('#'));
            if (mainKind.equals(transactionFilingMode)) {
                resources.put(resourceEntry.getKey(), resourceEntry.getValue());
            }
        }
        if (resources.isEmpty()) {
            String errorMessage = ("filing mode %s is set for a master resource but no resource on " +
                    "transaction URL %s has that kind").formatted(transactionFilingMode, transactionUrl);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        } else if (resources.size() > 1) {
            String errorMessage = ("filing mode %s is set for a master resource but more than one" +
                    " resource on the transaction URL %s has that kind").
                    formatted(transactionFilingMode, transactionUrl);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        }
    }

}
