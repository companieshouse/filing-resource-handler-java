package uk.gov.companieshouse.filingresourcehandler.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.api.model.filinggenerator.FilingApi;
import uk.gov.companieshouse.filing.received.Transaction;
import uk.gov.companieshouse.filingresourcehandler.factory.ItemFactory;
import uk.gov.companieshouse.filingresourcehandler.util.RetryErrorHandler;

import java.util.List;

public class ItemService {

    private final ItemFactory itemFactory;
    private final ObjectMapper objectMapper;

    public ItemService(ItemFactory itemFactory, ObjectMapper objectMapper) {
        this.itemFactory = itemFactory;
        this.objectMapper = objectMapper;
    }

    public void addItems(FilingApi filing, String submissionId, List<Transaction> items) {
        String filingDataJson = null;
        try {
            filingDataJson = objectMapper.writeValueAsString(filing.getData());
        } catch (JsonProcessingException e) {
            String errorMessage = "Unable to write filingDataJson for submissionId :%s".formatted(submissionId);
            RetryErrorHandler.logAndThrowRetryableException(errorMessage);
        }
        uk.gov.companieshouse.filing.received.Transaction item = itemFactory.getItem(filing, submissionId, filingDataJson);
        items.add(item);
    }
}
