package uk.gov.companieshouse.filingresourcehandler.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
        filingPatchService.addFilingToPatch(new HashMap<>(), getFilingApi()
                , "1234", "/transaction", "5678");
    }
}
