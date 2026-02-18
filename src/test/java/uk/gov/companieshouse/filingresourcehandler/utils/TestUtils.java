package uk.gov.companieshouse.filingresourcehandler.utils;


import accounts.transaction_closed;
import jakarta.validation.constraints.NotNull;
import uk.gov.companieshouse.filing.received.FilingReceived;
import uk.gov.companieshouse.filing.received.PresenterRecord;
import uk.gov.companieshouse.filing.received.SubmissionRecord;
import uk.gov.companieshouse.filing.received.Transaction;

import java.util.ArrayList;
import java.util.List;

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
        List<Transaction> transactions = new ArrayList<>();
        Transaction transaction = new Transaction();
        transaction.setData("Sample data");
        transaction.setKind("Sample kind");
        transaction.setSubmissionLanguage("ENG");
        transaction.setSubmissionId("SUB123456");
        transactions.add(transaction);

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

    public static transaction_closed getTransactionClosed() {
        return new transaction_closed(1, "http://example.com/transaction");
    }
}
