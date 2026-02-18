package uk.gov.companieshouse.filingresourcehandler.utils;


import accounts.transaction_closed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.NonNull;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.api.model.transaction.Filing;
import uk.gov.companieshouse.api.model.transaction.Resource;
import uk.gov.companieshouse.api.model.transaction.SubmittedBy;
import uk.gov.companieshouse.api.model.transaction.Transaction;
import uk.gov.companieshouse.api.model.transaction.TransactionLinks;
import uk.gov.companieshouse.api.model.transaction.TransactionStatus;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtils {

    @NotNull
    public static FilingReceived getFilingReceived() {
        PresenterRecord presenter = new PresenterRecord();
        presenter.setForename("John");
        presenter.setSurname("Doe");
        presenter.setLanguage("ENG");
        presenter.setUserId("123456");

        // Create SubmissionRecord
        SubmissionRecord submission = new SubmissionRecord();
        submission.setCompanyNumber("12345678");
        submission.setCompanyName("Example Company Ltd");
        submission.setReceivedAt("2023-10-01T12:00:00Z");
        submission.setTransactionId("TXN123456");

        // Create a list of Transaction objects
        List<uk.gov.companieshouse.filing.received.Transaction> transactions = getTransactions();

        // Create FilingReceived
        FilingReceived filingReceived = new FilingReceived();
        filingReceived.setApplicationId("APP123456");
        filingReceived.setAttempt(1);
        filingReceived.setChannelId("WEB");
        filingReceived.setPresenter(presenter);
        filingReceived.setSubmission(submission);
        filingReceived.setItems(transactions);
        return filingReceived;
    }

    private static @NonNull List<uk.gov.companieshouse.filing.received.Transaction> getTransactions() {
        List<uk.gov.companieshouse.filing.received.Transaction> transactions = new ArrayList<>();
        uk.gov.companieshouse.filing.received.Transaction transaction = new uk.gov.companieshouse.filing.received.Transaction();
        transaction.setData("Sample data");
        transaction.setKind("Sample kind");
        transaction.setSubmissionLanguage("ENG");
        transaction.setSubmissionId("SUB123456");
        transactions.add(transaction);
        return transactions;
    }

    public static transaction_closed getTransactionClosed() {
        return new transaction_closed(1, "transactions/987654");
    }

    @NotNull
    public static Transaction getTransactionRequestBody() throws JsonProcessingException {
// Create the Transaction object
        Transaction transaction = new Transaction();

// Set simple string fields
        transaction.setId("987654");
        transaction.setReference("LimitedPartnershipsReference");
        transaction.setStatus(TransactionStatus.CLOSED); // Use the correct enum value
        transaction.setKind("transaction");
        transaction.setCompanyName("PV2 CIDEV 03/03 LIMITED PARTNERSHIP");
        transaction.setCompanyNumber("LP5678");
        transaction.setCreatedAt("2026-03-03T16:32:46Z");
        transaction.setClosedAt("2026-03-03T16:33:00Z");
        transaction.setUpdatedAt("2026-03-03T16:33:00Z");
        transaction.setDescription("Designate as a private fund limited partnership");
        transaction.setFilingMode("default");
        transaction.setResumeJourneyUri("/limited-partnerships/update/company/LP5678/transaction/987654/submission/87qwerty/resume");

// Set closed_by and created_by as Map<String, String>
        Map<String, String> closedBy = new HashMap<>();
        closedBy.put("language", "en");
        closedBy.put("id", "bn6Q123456");
        closedBy.put("email", "pvodden@companieshouse.gov.uk");
        transaction.setClosedBy(closedBy);

        Map<String, String> createdBy = new HashMap<>();
        createdBy.put("language", "en");
        createdBy.put("id", "bn6Q123456");
        createdBy.put("email", "pvodden@companieshouse.gov.uk");
        transaction.setCreatedBy(createdBy);

// Set filings
        Result result = getResult();

// Set links for the filing
        Map<String, String> filingLinks = new HashMap<>();
        filingLinks.put("resource", "/transactions/987654/limited-partnership/partnership/87qwerty");
        result.filing().setLinks(filingLinks);

        result.filings().put("987654-1", result.filing());
        transaction.setFilings(result.filings());

// Set resources
        Map<String, Resource> resources = getStringResourceMap();
        transaction.setResources(resources);

// Set links (TransactionLinks)
        TransactionLinks transactionLinks = new TransactionLinks();
        transactionLinks.setSelf("/transactions/987654");
        transactionLinks.setPayment("/transactions/987654/payment");
        transaction.setLinks(transactionLinks);

// Set submitted_by
        SubmittedBy submittedBy = new SubmittedBy();
        submittedBy.setApplicationId("309912450514.apps.ch.gov.uk");
        submittedBy.setUserId("bn6Q123456");
        transaction.setSubmittedBy(submittedBy);
        return transaction;
    }

    private static @NonNull Map<String, Resource> getStringResourceMap() {
        Map<String, Resource> resources = new HashMap<>();
        Resource resource = new Resource();
        resource.setKind("limited-partnership#update-partnership-redesignate-to-pflp");

// Set links for the resource
        Map<String, String> resourceLinks = new HashMap<>();
        resourceLinks.put("costs", "/transactions/987654/limited-partnership/partnership/87qwerty/costs");
        resourceLinks.put("resource", "/transactions/987654/limited-partnership/partnership/87qwerty");
        resourceLinks.put("validation_status", "/transactions/987654/limited-partnership/partnership/87qwerty/validation-status");
        resource.setLinks(resourceLinks);

        resources.put("/transactions/987654/limited-partnership/partnership/87qwerty", resource);
        return resources;
    }

    private static @NonNull Result getResult() {
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = new Filing();
        filing.setDescription("Post Transition a Limited Partnership");
        filing.setStatus("accepted");
        filing.setType("limited-partnership-post-transition#update-partnership-redesignate-to-pflp");
        filing.setCost("1.00");
        filing.setCompanyNumber("LP5678");
        filing.setDescriptionIdentifier("");
        filing.setDescriptionValues(new HashMap<>());
        String dateString = "2026-03-03T16:36:34Z";
        ZonedDateTime zonedDateTime = ZonedDateTime.parse(dateString, DateTimeFormatter.ISO_DATE_TIME);
        filing.setProcessedAt(Date.from(zonedDateTime.toInstant()));
        return new Result(filings, filing);
    }

    private record Result(Map<String, Filing> filings, Filing filing) {
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }


    public static String getFilingApi() throws JsonProcessingException {
        FilingApi filingApi = new FilingApi();

// Set the data field as a Map<String, Object>
        Map<String, Object> data = new HashMap<>();
        Map<String, Object> limitedPartnership = new HashMap<>();
        limitedPartnership.put("partnership_number", "LP5678");
        limitedPartnership.put("partnership_name", "PV2 CIDEV 03/03 LIMITED PARTNERSHIP");
        limitedPartnership.put("email", null);
        limitedPartnership.put("partnership_type", "LP");
        limitedPartnership.put("jurisdiction", "england-wales");
        limitedPartnership.put("registered_office_address", null);
        limitedPartnership.put("term", null);
        limitedPartnership.put("redesignate_to_pflp_apply", true);
        limitedPartnership.put("redesignate_to_pflp_confirm", true);
        limitedPartnership.put("principal_place_of_business_address", null);
        limitedPartnership.put("sic_codes", null);
        limitedPartnership.put("lawful_purpose_statement_checked", null);
        limitedPartnership.put("kind", "limited-partnership#update-partnership-redesignate-to-pflp");
        limitedPartnership.put("date_of_update", "2026-03-03");
        limitedPartnership.put("company_previous_details", null);

        data.put("limited_partnership", limitedPartnership);
        data.put("payment_reference", "7lz0L12345");
        data.put("payment_method", "credit-card");

        filingApi.setData(data);

        filingApi.setDescription("Post Transition a Limited Partnership");
        filingApi.setDescriptionIdentifier(null); // or "" if your model expects a string
        filingApi.setDescriptionValues(new HashMap<>()); // empty map
        filingApi.setKind("limited-partnership-post-transition#update-partnership-redesignate-to-pflp");
        filingApi.setCost("1.00");
        FilingApi[] filings = new FilingApi[]{filingApi};
        ObjectMapper objectMapper = getObjectMapper();
        return objectMapper.writeValueAsString(filings);
    }

}
