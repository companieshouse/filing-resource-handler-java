package uk.gov.companieshouse.filingresourcehandler.kafka;

import accounts.transaction_closed;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;
import uk.gov.companieshouse.filingresourcehandler.service.FilingResourceHandlerService;

@Component
public class Consumer {
    private final FilingResourceHandlerService filingResourceHandlerService;
    private final MessageFlags messageFlags;

    public Consumer(FilingResourceHandlerService filingResourceHandlerService, MessageFlags messageFlags) {
        this.filingResourceHandlerService = filingResourceHandlerService;
        this.messageFlags = messageFlags;
    }

    @KafkaListener(
            id = "${message.group.name}",
            containerFactory = "messageSendKafkaListenerContainerFactory",
            topics = "${message.receive.topic}",
            groupId = "${message.group.name}"
    )
    public void consume(Message<transaction_closed> message) {
        try {
            filingResourceHandlerService.processMessage(message.getPayload());
        } catch (RetryableException ex) {
            messageFlags.setRetryable(true);
            throw ex;
        }
    }
}
