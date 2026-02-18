package uk.gov.companieshouse.filingresourcehandler.factory;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ResourceMapFactoryTest {

    @Test
    void testCreateResourceMapDefaultModeReturnsOriginalResources() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();
        Resource resource = new Resource();
        resource.setKind("default-kind");
        originalResources.put("key", resource);
        transaction.setResources(originalResources);
        transaction.setFilingMode("default");

        Map<String, Resource> result = factory.createResourceMap(transaction, "default-kind", "url");
        assertThat(result).isEqualTo(originalResources);
    }

    @Test
    void testCreateResourceMapCustomModeFiltersResources() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();
        Resource resource1 = new Resource();
        resource1.setKind("custom-kind#extra");
        Resource resource2 = new Resource();
        resource2.setKind("other-kind");
        originalResources.put("r1", resource1);
        originalResources.put("r2", resource2);
        transaction.setResources(originalResources);
        transaction.setFilingMode("custom-kind");

        Map<String, Resource> result = factory.createResourceMap(transaction, "custom-kind", "url");
        assertThat(result).containsOnlyKeys("r1");
        assertThat(result.get("r1").getKind()).startsWith("custom-kind");
    }
}

