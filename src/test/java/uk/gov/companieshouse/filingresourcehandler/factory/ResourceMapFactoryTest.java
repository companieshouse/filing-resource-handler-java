package uk.gov.companieshouse.filingresourcehandler.factory;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void testCreateResourceMapThrowsWhenNoResourceMatchesFilingMode() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();

        Resource resource = new Resource();
        resource.setKind("other-kind");
        originalResources.put("r1", resource);

        transaction.setResources(originalResources);
        transaction.setFilingMode("custom-kind");

        try (MockedStatic<RetryErrorHandler> mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RetryableException("retryable"));

            assertThrows(RetryableException.class,
                    () -> factory.createResourceMap(transaction, "custom-kind", "url"));

            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(
                    Mockito.argThat(msg -> msg.contains("no resource on transaction URL") && msg.contains("custom-kind"))));
        }
    }

    @Test
    void testCreateResourceMapThrowsWhenMoreThanOneResourceMatchesFilingMode() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();

        Resource resource1 = new Resource();
        resource1.setKind("custom-kind#one");
        Resource resource2 = new Resource();
        resource2.setKind("custom-kind#two");
        originalResources.put("r1", resource1);
        originalResources.put("r2", resource2);

        transaction.setResources(originalResources);
        transaction.setFilingMode("custom-kind");

        try (MockedStatic<RetryErrorHandler> mocked = Mockito.mockStatic(RetryErrorHandler.class)) {
            mocked.when(() -> RetryErrorHandler.logAndThrowRetryableException(Mockito.anyString()))
                    .thenThrow(new RetryableException("retryable"));

            assertThrows(RetryableException.class,
                    () -> factory.createResourceMap(transaction, "custom-kind", "url"));

            mocked.verify(() -> RetryErrorHandler.logAndThrowRetryableException(
                    Mockito.argThat(msg -> msg.contains("more than one") && msg.contains("custom-kind"))));
        }
    }

    @Test
    void testCreateResourceMapNullFilingModeReturnsOriginalResources() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();
        Resource resource = new Resource();
        resource.setKind("any-kind");
        originalResources.put("k", resource);
        transaction.setResources(originalResources);

        Map<String, Resource> result = factory.createResourceMap(transaction, null, "url");

        assertThat(result).isEqualTo(originalResources);
    }

    @Test
    void testCreateResourceMapBlankFilingModeReturnsOriginalResources() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();
        Resource resource = new Resource();
        resource.setKind("any-kind");
        originalResources.put("k", resource);
        transaction.setResources(originalResources);

        Map<String, Resource> result = factory.createResourceMap(transaction, "   ", "url");

        assertThat(result).isEqualTo(originalResources);
    }

    @Test
    void testCreateResourceMapReturnsEmptyMapWhenTransactionResourcesAreNull() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        transaction.setResources(null);

        Map<String, Resource> result = factory.createResourceMap(transaction, "default", "url");

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testCreateResourceMapCustomModeWithNullTransactionResourcesReturnsEmptyMap() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        transaction.setResources(null);
        Map<String, Resource> result = factory.createResourceMap(transaction, "custom-kind", "url");

        assertThat(result).isNotNull().isEmpty();
    }

    @Test
    void testCreateResourceMapSkipsNullResourceAndNullKindEntries() {
        ResourceMapFactory factory = new ResourceMapFactory();
        Transaction transaction = new Transaction();
        Map<String, Resource> originalResources = new HashMap<>();
        originalResources.put("null-resource", null);
        Resource nullKindResource = new Resource();
        originalResources.put("null-kind", nullKindResource);
        Resource matching = new Resource();
        matching.setKind("custom-kind");
        originalResources.put("match", matching);

        transaction.setResources(originalResources);

        Map<String, Resource> result = factory.createResourceMap(transaction, "custom-kind", "url");

        assertThat(result).containsOnlyKeys("match");
        assertThat(result.get("match").getKind()).isEqualTo("custom-kind");
    }
}

