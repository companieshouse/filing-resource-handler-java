package uk.gov.companieshouse.filingresourcehandler.factory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FilingReceivedFactory {

    private final String oauthClientId;
    private final ObjectMapper objectMapper;

    public FilingReceivedFactory(@Value("${oauth2.client.id}") String oauthClientId, ObjectMapper objectMapper) {
        this.oauthClientId = oauthClientId;
        this.objectMapper = objectMapper;
    }

    public FilingReceived getFilingReceived(List<uk.gov.companieshouse.filing.received.Transaction> items, Transaction transaction) {
        PresenterRecord presenterRecord = createPresenterRecord(transaction);
        String companyName = transaction.getCompanyName();
        String companyNumber = transaction.getCompanyNumber();
        if (companyName.isEmpty() || companyNumber.isBlank()) {
            Map<String, Object> tempFiling = new HashMap<>();
            try {
                tempFiling = objectMapper.readValue(items.getFirst().getData(), new TypeReference<>() {
                });
            } catch (Exception err) {
                String errorMessage = "Unable to parse json for transaction id %s".formatted(transaction.getId());
                RetryErrorHandler.logAndThrowRetryableException(errorMessage);
            }
            if (tempFiling.get("company_number") != null) {
                companyNumber = tempFiling.get("company_number").toString();
            }
            if (tempFiling.get("company_name") != null) {
                companyName = tempFiling.get("company_name").toString();
            }
        }
        String channelId = transaction.getSubmittedBy().getApplicationId().equals(oauthClientId) ? "chs" : "api-filing";
        SubmissionRecord submissionRecord = getSubmissionRecord(companyName, companyNumber, transaction.getClosedAt(), transaction.getId());
        return new FilingReceived(transaction.getSubmittedBy().getApplicationId(), 0, channelId, presenterRecord, submissionRecord, items);
    }

    private SubmissionRecord getSubmissionRecord(String companyName, String companyNumber, String closedAt, String id) {
        return new SubmissionRecord(companyNumber, companyName, closedAt, id);
    }

    private PresenterRecord createPresenterRecord(Transaction transaction) {
        return new PresenterRecord(
                transaction.getClosedBy().get("forename"),
                transaction.getClosedBy().get("language"),
                transaction.getClosedBy().get("surname"),
                transaction.getClosedBy().get("id")
        );
    }
}
