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
    private static final String TESTING_DESCRIPTION = "testing description";
    private static final String MULTI_TX_ID = "123456-789123-456789";

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

    @Test
    void addFilingToPatchSupportsSequentialAddsAndOverwriteForSameSubmissionId() {
        FilingApi filing1 = new FilingApi();
        filing1.setDescription(TESTING_DESCRIPTION);
        filing1.setDescriptionIdentifier("abridged");
        filing1.setDescriptionValues(Map.of(
                "next_made_up_on", "2017-08-08",
                "last_made_up_on", "2016-08-08"));
        filing1.setKind("accounts#abridged");

        FilingApi filing2 = new FilingApi();
        filing2.setDescription(TESTING_DESCRIPTION);
        filing2.setDescriptionIdentifier("ad01");
        filing2.setDescriptionValues(Map.of("at_address_from", "2016-08-08"));
        filing2.setKind("registered-office-address");

        FilingApi filing3 = new FilingApi();
        filing3.setDescription(TESTING_DESCRIPTION);
        filing3.setDescriptionIdentifier("600");
        filing3.setDescriptionValues(Map.of());
        filing3.setKind("insolvency#600");
        filing3.setData(Map.of(COMPANY_NUMBER_KEY, "01234567", "company_name", "TestCorp"));

        String companyNumber = "11223344";
        String submissionId1 = MULTI_TX_ID + "-1";
        String submissionId2 = MULTI_TX_ID + "-2";
        String link1 = "/transaction/" + MULTI_TX_ID + "/accounts/2016-12-12/abridged-accounts";
        String link2 = "/transaction/" + MULTI_TX_ID + "/registered-office-address";
        String link3 = "/transactions/" + MULTI_TX_ID + "/insolvency";

        when(filingFactory.getFiling(any(), any(), any())).thenAnswer(invocation -> {
            FilingApi filingApi = invocation.getArgument(0);
            String capturedCompanyNumber = invocation.getArgument(1);
            Map<String, String> links = invocation.getArgument(2);
            Filing filing = new Filing();
            filing.setCompanyNumber(capturedCompanyNumber);
            filing.setLinks(links);
            filing.setStatus("processing");
            filing.setType(filingApi.getKind());
            filing.setDescription(filingApi.getDescription());
            filing.setDescriptionIdentifier(filingApi.getDescriptionIdentifier());
            filing.setDescriptionValues(filingApi.getDescriptionValues());
            return filing;
        });

        Map<String, Filing> filingsMap = new HashMap<>();

        filingPatchService.addFilingToPatch(filingsMap, filing1, submissionId1, link1, "");
        Assertions.assertEquals(1, filingsMap.size());
        Assertions.assertEquals("", filingsMap.get(submissionId1).getCompanyNumber());
        Assertions.assertEquals(link1, filingsMap.get(submissionId1).getLinks().get("resource"));
        Assertions.assertEquals("accounts#abridged", filingsMap.get(submissionId1).getType());
        Assertions.assertEquals("abridged", filingsMap.get(submissionId1).getDescriptionIdentifier());

        filingPatchService.addFilingToPatch(filingsMap, filing2, submissionId2, link2, companyNumber);
        Assertions.assertEquals(2, filingsMap.size());
        Assertions.assertEquals(companyNumber, filingsMap.get(submissionId2).getCompanyNumber());
        Assertions.assertEquals(link2, filingsMap.get(submissionId2).getLinks().get("resource"));
        Assertions.assertEquals("registered-office-address", filingsMap.get(submissionId2).getType());
        Assertions.assertEquals("ad01", filingsMap.get(submissionId2).getDescriptionIdentifier());

        // Reusing submissionId1 should overwrite the previous filing1 entry.
        filingPatchService.addFilingToPatch(filingsMap, filing3, submissionId1, link3, companyNumber);
        Assertions.assertEquals(2, filingsMap.size());
        Assertions.assertEquals("01234567", filingsMap.get(submissionId1).getCompanyNumber());
        Assertions.assertEquals(link3, filingsMap.get(submissionId1).getLinks().get("resource"));
        Assertions.assertEquals("insolvency#600", filingsMap.get(submissionId1).getType());
        Assertions.assertEquals("600", filingsMap.get(submissionId1).getDescriptionIdentifier());
    }
}
