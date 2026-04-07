package uk.gov.companieshouse.filingresourcehandler.utils;


import accounts.transaction_closed;
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
import uk.gov.companieshouse.filingresourcehandler.factory.FilingFactory;
import uk.gov.companieshouse.filingresourcehandler.factory.FilingReceivedFactory;
import uk.gov.companieshouse.filingresourcehandler.factory.ItemFactory;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestUtils {
    // Common test constants for repeated values
    public static final String TEST_TRANSACTIONS_KEY = "/transactions/987654/limited-partnership/partnership/87qwerty";
    private static final String TEST_TRANSACTION_ID = "987654";
    private static final String TEST_COMPANY_NUMBER = "LP5678";
    private static final String TEST_COMPANY_NAME = "PV2 CIDEV 03/03 LIMITED PARTNERSHIP";
    private static final String TEST_KIND = "limited-partnership#update-partnership-redesignate-to-pflp";
    private static final String TEST_USER_ID = "bn6Q123456";
    private static final String TEST_EMAIL = "pvodden@companieshouse.gov.uk";
    private static final String TEST_APPLICATION_ID = "309912450514.apps.ch.gov.uk";
    private static final String TEST_RESUME_JOURNEY_URI = "/limited-partnerships/update/company/LP5678/transaction/987654/submission/87qwerty/resume";
    private static final String TEST_SELF_LINK = "/transactions/987654";
    private static final String TEST_PAYMENT_LINK = "/transactions/987654/payment";
    private static final String TEST_RESOURCE_PATH = "/transactions/987654/limited-partnership/partnership/87qwerty";
    private static final String TEST_COSTS_PATH = "/transactions/987654/limited-partnership/partnership/87qwerty/costs";
    private static final String TEST_VALIDATION_STATUS_PATH = "/transactions/987654/limited-partnership/partnership/87qwerty/validation-status";
    private static final String TEST_FILINGS_KEY = "987654-1";
    public static final String GET_API_CALL = "GET call to transaction";
    public static final String PATCH_API_CALL = "Patch call to transaction";

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
        List<uk.gov.companieshouse.filing.received.Transaction> transactions = getFilingReceivedTransactions();

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

    private static @NonNull List<uk.gov.companieshouse.filing.received.Transaction> getFilingReceivedTransactions() {
        List<uk.gov.companieshouse.filing.received.Transaction> transactions = new ArrayList<>();
        uk.gov.companieshouse.filing.received.Transaction transaction = new uk.gov.companieshouse.filing.received.Transaction();
        transaction.setData("Sample data");
        transaction.setKind("Sample kind");
        transaction.setSubmissionLanguage("ENG");
        transaction.setSubmissionId("SUB123456");
        transactions.add(transaction);
        return transactions;
    }

    public static transaction_closed getTransactionClosedMessage() {
        return new transaction_closed(1, "transactions/987654");
    }

    @NotNull
    public static Transaction getTransaction() {
// Create the Transaction object
        Transaction transaction = new Transaction();

// Set simple string fields
        transaction.setId(TEST_TRANSACTION_ID);
        transaction.setReference("LimitedPartnershipsReference");
        transaction.setStatus(TransactionStatus.CLOSED); // Use the correct enum value
        transaction.setKind("transaction");
        transaction.setCompanyName(TEST_COMPANY_NAME);
        transaction.setCompanyNumber(TEST_COMPANY_NUMBER);
        transaction.setCreatedAt("2026-03-03T16:32:46Z");
        transaction.setClosedAt("2026-03-03T16:33:00Z");
        transaction.setUpdatedAt("2026-03-03T16:33:00Z");
        transaction.setDescription("Designate as a private fund limited partnership");
        transaction.setFilingMode("default");
        transaction.setResumeJourneyUri(TEST_RESUME_JOURNEY_URI);

// Set closed_by and created_by as Map<String, String>
        Map<String, String> closedBy = new HashMap<>();
        closedBy.put("language", "en");
        closedBy.put("id", TEST_USER_ID);
        closedBy.put("email", TEST_EMAIL);
        transaction.setClosedBy(closedBy);

        Map<String, String> createdBy = new HashMap<>();
        createdBy.put("language", "en");
        createdBy.put("id", TEST_USER_ID);
        createdBy.put("email", TEST_EMAIL);
        transaction.setCreatedBy(createdBy);

// Set filings
        Result result = getResult();

// Set links for the filing
        Map<String, String> filingLinks = new HashMap<>();
        filingLinks.put("resource", TEST_RESOURCE_PATH);
        result.filing().setLinks(filingLinks);

        result.filings().put(TEST_FILINGS_KEY, result.filing());
        transaction.setFilings(result.filings());

// Set resources
        Map<String, Resource> resources = getStringResourceMap();
        transaction.setResources(resources);

// Set links (TransactionLinks)
        TransactionLinks transactionLinks = new TransactionLinks();
        transactionLinks.setSelf(TEST_SELF_LINK);
        transactionLinks.setPayment(TEST_PAYMENT_LINK);
        transaction.setLinks(transactionLinks);

// Set submitted_by
        SubmittedBy submittedBy = new SubmittedBy();
        submittedBy.setApplicationId(TEST_APPLICATION_ID);
        submittedBy.setUserId(TEST_USER_ID);
        transaction.setSubmittedBy(submittedBy);
        return transaction;
    }

    public static Map<String, Resource> getStringResourceMap() {
        Map<String, Resource> resources = new HashMap<>();
        Resource resource = new Resource();
        resource.setKind(TEST_KIND);

// Set links for the resource
        Map<String, String> resourceLinks = new HashMap<>();
        resourceLinks.put("costs", TEST_COSTS_PATH);
        resourceLinks.put("resource", TEST_RESOURCE_PATH);
        resourceLinks.put("validation_status", TEST_VALIDATION_STATUS_PATH);
        resource.setLinks(resourceLinks);
        resources.put(TEST_RESOURCE_PATH, resource);
        return resources;
    }

    private static @NonNull Result getResult() {
        Map<String, Filing> filings = new HashMap<>();
        Filing filing = getFiling();
        return new Result(filings, filing);
    }

    public static @NonNull Filing getFiling() {
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
        return filing;
    }

    private record Result(Map<String, Filing> filings, Filing filing) {
    }

    public static ObjectMapper getObjectMapper() {
        return new ObjectMapper();
    }


    public static FilingApi[] getFilingApiList() {
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
        return new FilingApi[]{filingApi};
    }

    public static FilingFactory getFilingFactory() {
        return new FilingFactory();
    }


    public static HashMap<String, String> getLinks() {
        HashMap<String, String> links = new HashMap<>();
        links.put("resource", "/resource/link");
        return links;
    }

    public static ItemFactory getItemFactory() {
        return new ItemFactory();
    }

    public static FilingApi getFilingApi() {
        FilingApi filingApi = new FilingApi();
        filingApi.setKind("test-kind");
        filingApi.setDescription("desc");
        filingApi.setDescriptionIdentifier("id");
        filingApi.setDescriptionValues(new HashMap<>());
        filingApi.setCost("10.00");
        filingApi.setData(new HashMap<>());
        return filingApi;
    }

    public static FilingReceivedFactory getFilingReceivedFactory(String oauthClientId) {
        return new FilingReceivedFactory(oauthClientId, getObjectMapper());
    }

    public static Transaction getTransactionForFilingReceived() {
        Transaction transaction = new Transaction();
        transaction.setCompanyName("Test Company");
        transaction.setCompanyNumber("12345678");
        transaction.setClosedAt("2026-03-03T16:33:00Z");
        transaction.setId("txn-id");
        Map<String, String> closedBy = new HashMap<>();
        closedBy.put("forename", "John");
        closedBy.put("language", "en");
        closedBy.put("surname", "Doe");
        closedBy.put("id", "user-id");
        transaction.setClosedBy(closedBy);
        return transaction;
    }

    public static SubmissionRecord getSubmissionRecord() {
        return new SubmissionRecord("12345678", "Test Company", "2026-03-03T16:33:00Z", "txn-id");
    }

    public static PresenterRecord getPresenterRecord() {
        return new PresenterRecord("John", "en", "Doe", "user-id");
    }

    public static List<uk.gov.companieshouse.filing.received.Transaction> getEmptyItemsList() {
        return Collections.emptyList();
    }

    public static uk.gov.companieshouse.filing.received.Transaction getTransactionItemWithCompanyData() {
        uk.gov.companieshouse.filing.received.Transaction item = new uk.gov.companieshouse.filing.received.Transaction();
        String json = "{\"company_number\":\"12345678\",\"company_name\":\"Test Company\"}";
        item.setData(json);
        return item;
    }

    public static Transaction getTransactionWithEmptyCompanyFields() {
        Transaction transaction = new Transaction();
        transaction.setCompanyName("");
        transaction.setCompanyNumber("");
        transaction.setId("txn-id");
        return transaction;
    }

    public static Map<String, String> getClosedByMap() {
        Map<String, String> closedBy = new HashMap<>();
        closedBy.put("forename", "John");
        closedBy.put("language", "en");
        closedBy.put("surname", "Doe");
        closedBy.put("id", "user-id");
        return closedBy;
    }
}
