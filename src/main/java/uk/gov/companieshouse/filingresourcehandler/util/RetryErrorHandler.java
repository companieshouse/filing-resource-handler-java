package uk.gov.companieshouse.filingresourcehandler.util;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

@Component
public class RetryErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(NAMESPACE);

    public static void logAndThrowRetryableException(String message) {
        logger.error(message, DataMapHolder.getLogMap());
        throw new RetryableException(message);
    }

}
