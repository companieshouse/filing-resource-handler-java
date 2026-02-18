package uk.gov.companieshouse.filingresourcehandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.apiclient.FilingClient;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;
import uk.gov.companieshouse.filingresourcehandler.factory.ItemFactory;
import uk.gov.companieshouse.filingresourcehandler.model.FilingProcessingResult;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilingResourceProcessorServiceTest {

    @Mock
    private FilingClient filingClient;
    @Mock
    private FilingFactory filingFactory;
    @Mock
    private ItemFactory itemFactory;
    @Mock
    ObjectMapper objectMapper;

    @InjectMocks
    private FilingResourceProcessorService service;

    @Test
    void processResourcesReturnsExpectedResult() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId("txn-id");
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        Map<String, Filing> filingsMap = new HashMap<>();
        transaction.setFilings(filingsMap);

        Resource resource = new Resource();
        Map<String, String> links = new HashMap<>();
        links.put("resource", "resource-link");
        resource.setLinks(links);
        resource.setKind("test-kind");
        Map<String, Resource> resources = new HashMap<>();
        resources.put("r1", resource);

        FilingApi filingApi = new FilingApi();
        filingApi.setKind("test-kind");
        filingApi.setDescription("desc");
        filingApi.setDescriptionIdentifier("id");
        filingApi.setDescriptionValues(new HashMap<>());
        filingApi.setCost("10.00");
        filingApi.setData(new HashMap<>());
        FilingApi[] filingApis = new FilingApi[]{filingApi};

        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(Optional.of(filingApis));

        Filing filing = new Filing();
        filing.setCompanyNumber("12345678");
        filing.setDescription("desc");
        filing.setDescriptionIdentifier("id");
        filing.setDescriptionValues(new HashMap<>());
        filing.setLinks(links);
        filing.setStatus("processing");
        filing.setType("test-kind");
        filing.setCost("10.00");
        when(filingFactory.getFiling(any(), any(), any())).thenReturn(filing);

        uk.gov.companieshouse.filing.received.Transaction item = new uk.gov.companieshouse.filing.received.Transaction();
        item.setData("{}");
        item.setKind("test-kind");
        item.setSubmissionId("txn-id-1");
        item.setSubmissionLanguage("en");
        when(itemFactory.getItem(any(), any(), any())).thenReturn(item);

        // Act
        FilingProcessingResult result = service.processResources(transaction, resources);

        // Assert
        assertThat(result.getFilingsToPatch()).isEmpty(); // filingsToPatch is not used in this implementation
        assertThat(result.getItems()).hasSize(1);
        assertThat(result.getItems().getFirst().getKind()).isEqualTo("test-kind");
        assertThat(result.getItems().getFirst().getSubmissionId()).isEqualTo("txn-id-1");
    }

    @Test
    void processResourcesThrowsRetryableExceptionWhenFilingApiIsEmpty() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId("txn-id");
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        Map<String, Filing> filingsMap = new HashMap<>();
        transaction.setFilings(filingsMap);

        Resource resource = new Resource();
        Map<String, String> links = new HashMap<>();
        links.put("resource", "resource-link");
        resource.setLinks(links);
        resource.setKind("test-kind");
        Map<String, Resource> resources = new HashMap<>();
        resources.put("r1", resource);

        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.processResources(transaction, resources))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Empty Filings response for transactionId");
    }

    @Test
    void processResourcesThrowsRetryableExceptionWhenFilingDataJsonFails() throws com.fasterxml.jackson.core.JsonProcessingException {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId("txn-id");
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        Map<String, Filing> filingsMap = new HashMap<>();
        transaction.setFilings(filingsMap);

        Resource resource = new Resource();
        Map<String, String> links = new HashMap<>();
        links.put("resource", "resource-link");
        resource.setLinks(links);
        resource.setKind("test-kind");
        Map<String, Resource> resources = new HashMap<>();
        resources.put("r1", resource);

        JsonProcessingException mockException = new JsonProcessingException("Mocked error") {
        };
        FilingApi filingApi = Mockito.mock(FilingApi.class);
        when(filingApi.getKind()).thenReturn("ewf");
        when(objectMapper.writeValueAsString(any())).thenThrow(mockException);
        FilingApi[] filingApis = new FilingApi[]{filingApi};
        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(Optional.of(filingApis));

        // Act & Assert
        assertThatThrownBy(() -> service.processResources(transaction, resources))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Unable to write filingDataJson for submissionId");
    }

    @Test
    void processResourcesThrowsRetryableExceptionWhenSubmissionIdOffsetIsInvalid() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId("txn-id");
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        Map<String, Filing> filingsMap = new HashMap<>();
        Filing filing = new Filing();
        filing.setType("test-kind");
        Map<String, String> links = new HashMap<>();
        links.put("resource", "resource-link");
        filing.setLinks(links);
        filingsMap.put("invalid-submission-id", filing); // This will cause NumberFormatException
        transaction.setFilings(filingsMap);

        Resource resource = new Resource();
        resource.setLinks(links);
        resource.setKind("test-kind");
        Map<String, Resource> resources = new HashMap<>();
        resources.put("r1", resource);

        // Act & Assert
        assertThatThrownBy(() -> service.processResources(transaction, resources))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Invalid offset in submissionID");
    }
}
