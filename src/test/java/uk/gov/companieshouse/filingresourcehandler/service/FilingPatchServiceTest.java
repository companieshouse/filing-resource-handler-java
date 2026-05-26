package uk.gov.companieshouse.filingresourcehandler.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.exception.NonRetryableException;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFiling;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingApi;

@ExtendWith(MockitoExtension.class)
class FilingPatchServiceTest {
    private static final String INSOLVENCY_KIND = "company-insolvency#test";
    private static final String MISSING_COMPANY_NUMBER_MESSAGE = "Missing company_number";
    private static final String ORIGINAL_COMPANY_NUMBER = "ORIGINAL";
    private static final String COMPANY_NUMBER_KEY = "company_number";
    private static final String RESOURCE_LINK = "/transactions/resource";
    private static final String SUBMISSION_ID = "submission-id";

    @Mock
    private FilingFactory filingFactory;

    @InjectMocks
    private FilingPatchService filingPatchService;

    @Test
    void addFilingToPatchSuccessfully() {
        when(filingFactory.getFiling(any(), any(), any())).thenReturn(getFiling());
        HashMap<String, Filing> map = new HashMap<>();
        filingPatchService.addFilingToPatch(map, getFilingApi(), SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER);
        // Assert that the map contains the expected key and value
        Assertions.assertTrue(map.containsKey(SUBMISSION_ID));
        Filing filing = map.get(SUBMISSION_ID);
        Assertions.assertNotNull(filing);
        Assertions.assertEquals(getFiling().getType(), filing.getType());
    }

    @Test
    void addFilingToPatchUsesInsolvencyCompanyNumberAndResourceLinkMap() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind(INSOLVENCY_KIND);
        filingApi.setData(Map.of(COMPANY_NUMBER_KEY, "LP5678"));

        when(filingFactory.getFiling(any(), any(), any())).thenReturn(getFiling());

        HashMap<String, Filing> map = new HashMap<>();
        filingPatchService.addFilingToPatch(map, filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER);

        ArgumentCaptor<Map<String, String>> linksCaptor = ArgumentCaptor.captor();
        verify(filingFactory).getFiling(eq(filingApi), eq("LP5678"), linksCaptor.capture());
        Assertions.assertEquals(RESOURCE_LINK, linksCaptor.getValue().get("resource"));
    }

    @Test
    void addFilingToPatchUsesOriginalCompanyNumberForNonInsolvencyKind() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind("accounts#test");
        filingApi.setData(Map.of(COMPANY_NUMBER_KEY, "SHOULD_NOT_BE_USED"));
        when(filingFactory.getFiling(any(), any(), any())).thenReturn(getFiling());

        filingPatchService.addFilingToPatch(new HashMap<>(), filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER);

        ArgumentCaptor<Map<String, String>> linksCaptor = ArgumentCaptor.captor();
        verify(filingFactory).getFiling(eq(filingApi), eq(ORIGINAL_COMPANY_NUMBER), linksCaptor.capture());
        Assertions.assertEquals(RESOURCE_LINK, linksCaptor.getValue().get("resource"));
    }

    @Test
    void addFilingToPatchThrowsWhenInsolvencyDataIsNull() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind(INSOLVENCY_KIND);
        Map<String, Filing> filings = new HashMap<>();

        NonRetryableException exception = Assertions.assertThrows(
                NonRetryableException.class,
                () -> filingPatchService.addFilingToPatch(
                        filings, filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER));

        Assertions.assertTrue(exception.getMessage().contains(MISSING_COMPANY_NUMBER_MESSAGE));
    }

    @Test
    void addFilingToPatchThrowsWhenInsolvencyDataHasNoCompanyNumber() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind(INSOLVENCY_KIND);
        filingApi.setData(Map.of("other_field", "value"));
        Map<String, Filing> filings = new HashMap<>();

        NonRetryableException exception = Assertions.assertThrows(
                NonRetryableException.class,
                () -> filingPatchService.addFilingToPatch(
                        filings, filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER));

        Assertions.assertTrue(exception.getMessage().contains(MISSING_COMPANY_NUMBER_MESSAGE));
    }

    @Test
    void addFilingToPatchThrowsWhenInsolvencyCompanyNumberIsBlank() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind(INSOLVENCY_KIND);
        filingApi.setData(Map.of(COMPANY_NUMBER_KEY, "   "));
        Map<String, Filing> filings = new HashMap<>();

        NonRetryableException exception = Assertions.assertThrows(
                NonRetryableException.class,
                () -> filingPatchService.addFilingToPatch(
                        filings, filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER));

        Assertions.assertTrue(exception.getMessage().contains(MISSING_COMPANY_NUMBER_MESSAGE));
    }


    @Test
    void addFilingToPatchKeepsOriginalCompanyNumberWhenKindIsNull() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind(null);
        filingApi.setData(Map.of(COMPANY_NUMBER_KEY, "SHOULD_NOT_BE_USED"));

        when(filingFactory.getFiling(any(), any(), any())).thenReturn(getFiling());

        filingPatchService.addFilingToPatch(new HashMap<>(), filingApi, SUBMISSION_ID, RESOURCE_LINK, ORIGINAL_COMPANY_NUMBER);

        verify(filingFactory).getFiling(eq(filingApi), eq(ORIGINAL_COMPANY_NUMBER), any());
    }
}
