package uk.gov.companieshouse.filingresourcehandler.factory;

import static uk.gov.companieshouse.filingresourcehandler.Application.NAMESPACE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.transaction.SubmittedBy;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.logging.DataMapHolder;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class FilingReceivedFactory {

    private static final Logger logger = LoggerFactory.getLogger(NAMESPACE);

    private static final String CHANNEL_CHS = "chs";
    private static final String CHANNEL_API_FILING = "api-filing";
    private static final String FIELD_COMPANY_NUMBER = "company_number";
    private static final String FIELD_COMPANY_NAME = "company_name";
    private static final String FIELD_FORENAME = "forename";
    private static final String FIELD_LANGUAGE = "language";
    private static final String FIELD_SURNAME = "surname";
    private static final String FIELD_ID = "id";
    private static final int DEFAULT_ATTEMPT = 0;

    private final String oauthClientId;
    private final ObjectMapper objectMapper;

    public FilingReceivedFactory(@Value("${oauth2.client.id}") String oauthClientId,
            ObjectMapper objectMapper) {
        this.oauthClientId = oauthClientId;
        this.objectMapper = objectMapper;
    }

    public FilingReceived getFilingReceived(List<uk.gov.companieshouse.filing.received.Transaction> items,
            Transaction transaction) {
        if (transaction == null) {
            //Added to counter SonarQube issue as SonarQube was unable to detect logAndThrowRetryableException
            String message = "Transaction is null in getFilingReceived";
            logger.error(message, DataMapHolder.getLogMap());
            throw new RetryableException(message);
        }
        String transactionId = transaction.getId() != null ? transaction.getId() : "";

        PresenterRecord presenterRecord = createPresenterRecord(transaction);
        String companyName = transaction.getCompanyName();
        String companyNumber = transaction.getCompanyNumber();

        if (StringUtils.isBlank(companyName)) {
            companyName = handleEmptyField(FIELD_COMPANY_NAME, items, transactionId);
        }

        if (StringUtils.isBlank(companyNumber)) {
            companyNumber = handleEmptyField(FIELD_COMPANY_NUMBER, items, transactionId);
        }

        String applicationId = Optional.ofNullable(transaction.getSubmittedBy())
                .map(SubmittedBy::getApplicationId)
                .orElse("");

        String channelId = Objects.equals(applicationId, oauthClientId)
                ? CHANNEL_CHS
                : CHANNEL_API_FILING;

        SubmissionRecord submissionRecord = new SubmissionRecord(
                companyNumber, companyName, transaction.getClosedAt(), transactionId);

        return new FilingReceived(
                applicationId,
                DEFAULT_ATTEMPT,
                channelId,
                presenterRecord,
                submissionRecord,
                items);
    }

    private PresenterRecord createPresenterRecord(Transaction transaction) {
        Map<String, String> closedBy = transaction.getClosedBy() != null
                ? transaction.getClosedBy()
                : Map.of();

        return new PresenterRecord(
                closedBy.get(FIELD_FORENAME),
                closedBy.get(FIELD_LANGUAGE),
                closedBy.get(FIELD_SURNAME),
                closedBy.get(FIELD_ID));
    }

    private String handleEmptyField(String fieldKey, List<uk.gov.companieshouse.filing.received.Transaction> items,
            String transactionId) {
        String fieldValue = "";
        if (items.getFirst().getData() == null) {
            String errorMessage = "Items list has no data for transaction id %s".formatted(transactionId);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        }
        Map<String, Object> tempFiling = Map.of();
        try {
            tempFiling = objectMapper.readValue(
                    items.getFirst().getData(),
                    new TypeReference<>() {
                    });
        } catch (Exception err) {
            String errorMessage = "Unable to parse json for transaction id %s".formatted(transactionId);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        }
        Map<String, Object> filingData = Optional.ofNullable(tempFiling).orElse(Map.of());
        Object fieldObject = filingData.get(fieldKey);
        if (fieldObject != null) {
            fieldValue = fieldObject.toString();
        }
        return fieldValue;
    }
}
