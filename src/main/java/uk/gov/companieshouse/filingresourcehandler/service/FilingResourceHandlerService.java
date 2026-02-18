package uk.gov.companieshouse.filingresourcehandler.service;

import accounts.transaction_closed;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Service
public class FilingResourceHandlerService {
    private static final Logger LOGGER = LoggerFactory.getLogger(NAMESPACE);

    public void processMessage(transaction_closed transactionClosed) {
        LOGGER.info("Processing Transaction Closed message with url: %s".formatted(transactionClosed.getTransactionUrl()));
    }
}
