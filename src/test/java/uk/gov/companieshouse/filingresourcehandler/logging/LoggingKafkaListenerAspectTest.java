package uk.gov.companieshouse.filingresourcehandler.logging;

import accounts.transaction_closed;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import uk.gov.companieshouse.filingresourcehandler.exception.RetryableException;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingKafkaListenerAspectTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private Message<transaction_closed> message;

    @Mock
    private MessageHeaders headers;

    @Mock
    private transaction_closed transactionClosed;
    private LoggingKafkaListenerAspect loggingKafkaListenerAspect;

    @BeforeEach
    void setUp() {
        loggingKafkaListenerAspect = new LoggingKafkaListenerAspect(3);
        DataMapHolder.clear();
    }

    @Test
    void manageStructuredLoggingProcessesMessageSuccessfully() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(message.getHeaders()).thenReturn(headers);
        when(headers.get(anyString())).thenReturn(null);
        when(message.getPayload()).thenReturn(transactionClosed);
        when(joinPoint.proceed()).thenReturn("result");

        Object result = loggingKafkaListenerAspect.manageStructuredLogging(joinPoint);

        assertEquals("result", result);
        assertNull(DataMapHolder.getLogMap().get("message_id")); // DataMapHolder should be cleared
    }

    @Test
    void manageStructuredLoggingHandlesRetryableExceptionWithMaxAttempts() throws Throwable {
        byte[] attemptsBytes = ByteBuffer.allocate(4).putInt(3).array(); // 3 attempts
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(message.getHeaders()).thenReturn(headers);
        when(headers.get("retry_topic-attempts")).thenReturn(attemptsBytes);
        when(headers.get("kafka_receivedTopic")).thenReturn("topic");
        when(headers.get("kafka_receivedPartitionId")).thenReturn(1);
        when(headers.get("kafka_offset")).thenReturn(10L);
        when(message.getPayload()).thenReturn(transactionClosed);

        when(joinPoint.proceed()).thenThrow(new RetryableException("retry"));

        RetryableException thrown = assertThrows(RetryableException.class, () -> loggingKafkaListenerAspect.manageStructuredLogging(joinPoint));
        assertEquals("retry", thrown.getMessage());
    }

    @Test
    void manageStructuredLoggingHandlesRetryableExceptionWithLessThanMaxAttempts() throws Throwable {
        byte[] attemptsBytes = ByteBuffer.allocate(4).putInt(2).array(); // 2 attempts
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(message.getHeaders()).thenReturn(headers);
        when(headers.get("retry_topic-attempts")).thenReturn(attemptsBytes);
        when(headers.get("kafka_receivedTopic")).thenReturn("topic");
        when(headers.get("kafka_receivedPartitionId")).thenReturn(1);
        when(headers.get("kafka_offset")).thenReturn(10L);
        when(message.getPayload()).thenReturn(transactionClosed);

        when(joinPoint.proceed()).thenThrow(new RetryableException("retry"));

        RetryableException thrown = assertThrows(RetryableException.class, () -> loggingKafkaListenerAspect.manageStructuredLogging(joinPoint));
        assertEquals("retry", thrown.getMessage());
    }

    @Test
    void manageStructuredLoggingHandlesNonRetryableException() throws Throwable {
        when(joinPoint.getArgs()).thenReturn(new Object[]{message});
        when(message.getHeaders()).thenReturn(headers);
        when(message.getPayload()).thenReturn(transactionClosed);

        when(joinPoint.proceed()).thenThrow(new RuntimeException("fail"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> loggingKafkaListenerAspect.manageStructuredLogging(joinPoint));
        assertEquals("fail", thrown.getMessage());
    }
}