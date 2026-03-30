package uk.gov.companieshouse.filingresourcehandler.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;

import java.util.HashMap;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFiling;
import static uk.gov.companieshouse.filingresourcehandler.utils.TestUtils.getFilingApi;

@ExtendWith(MockitoExtension.class)
class FilingPatchServiceTest {
    @Mock
    private FilingFactory filingFactory;

    @InjectMocks
    private FilingPatchService filingPatchService;

    @Test
    void addFilingToPatchSuccessfully() {
        when(filingFactory.getFiling(any(), any(), any())).thenReturn(getFiling());
        HashMap<String, Filing> map = new HashMap<>();
        filingPatchService.addFilingToPatch(map, getFilingApi(), "1234", "/transaction", "5678");
        // Assert that the map contains the expected key and value
        Assertions.assertTrue(map.containsKey("1234"));
        Filing filing = map.get("1234");
        Assertions.assertNotNull(filing);
        Assertions.assertEquals(getFiling().getType(), filing.getType());
    }
}
