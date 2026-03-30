package uk.gov.companieshouse.filingresourcehandler.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filingresourcehandler.apiclient.FilingClient;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.model.FilingProcessingResult;
import uk.gov.companieshouse.filingresourcehandler.utils.TestUtils;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingApiList;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getStringResourceMap;

@ExtendWith(MockitoExtension.class)
class FilingResourceProcessorServiceTest {

    @Mock
    private FilingClient filingClient;
    @Mock
    private ItemService itemService;
    @Mock
    private SubmissionIdService submissionIdService;
    @Mock
    private FilingPatchService filingPatchService;
    @InjectMocks
    private FilingResourceProcessorService service;

    @Test
    void processResourcesReturnsExpectedResult() {
        // Arrange
        Transaction transaction = TestUtils.getTransaction();

        Map<String, Resource> resources = getStringResourceMap();

        Optional<FilingApi[]> filingApis = Optional.of(getFilingApiList());

        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(filingApis);
        // Act
        FilingProcessingResult result = service.processResources(transaction, resources);

        // Assert
        assertThat(result.getFilingsToPatch()).isEmpty(); // filingsToPatch is not used in this implementation
        verify(itemService).addItems(any(), any(), any());
    }


    @Test
    void processResourcesThrowsRetryableExceptionWhenFilingApiIsEmpty() {
        // Arrange
        Transaction transaction = TestUtils.getTransaction();

        Map<String, Resource> resources = getStringResourceMap();

        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.processResources(transaction, resources))
                .isInstanceOf(RetryableException.class)
                .hasMessageContaining("Empty Filings response for transactionId");
    }


    @Test
    void processResourcesThrowsRetryableExceptionWhenSubmissionIdOffsetIsInvalid() {
        // Arrange
        Transaction transaction = TestUtils.getTransaction();

        Map<String, Resource> resources = getStringResourceMap();
        when(submissionIdService.findSubmissionIDOffset(any(), any())).thenThrow(RetryableException.class);
        // Act & Assert
        assertThatThrownBy(() -> service.processResources(transaction, resources))
                .isInstanceOf(RetryableException.class);
    }

    @Test
    void processResourcesDoesNotPatchWhenSubmissionIdExistsInMatcher() {
        // Arrange
        Transaction transaction = TestUtils.getTransaction();
        Map<String, Resource> resources = getStringResourceMap();
        Optional<FilingApi[]> filingApis = Optional.of(getFilingApiList());
        // Simulate transactionMatcher containing a submissionId for the kind/link
        when(submissionIdService.findSubmissionIDOffset(any(), any())).thenAnswer(invocation -> {
            Map<String, String> matcher = invocation.getArgument(1);
            // The FilingApi kind and resource link must match what processResources uses
            matcher.put("limited-partnership-post-transition#update-partnership-redesignate-to-pflp:/transactions/987654/limited-partnership/partnership/87qwerty", "existing-submission-id");
            return 1;
        });
        when(filingClient.getFilingApi(any(), any(), any())).thenReturn(filingApis);

        // Act
        FilingProcessingResult result = service.processResources(transaction, resources);

        // Assert
        assertThat(result.getFilingsToPatch()).isEmpty();
        verify(filingPatchService, never()).addFilingToPatch(any(), any(), any(), any(), any());
        verify(itemService).addItems(any(), any(), any());
    }
}
