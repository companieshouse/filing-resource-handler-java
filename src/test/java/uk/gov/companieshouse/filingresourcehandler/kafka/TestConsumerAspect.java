package uk.gov.companieshouse.filingresourcehandler.kafka;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;

@Aspect
@Component
public class TestConsumerAspect {

    private final int steps;
    private CountDownLatch latch;

    public TestConsumerAspect(@Value("${steps:1}") int steps) {
        this.steps = steps;
        this.latch = new CountDownLatch(steps);
    }

    @After("execution(* Consumer.consume(..))")
    void afterConsume(JoinPoint joinPoint) {
        latch.countDown();
    }

    public CountDownLatch getLatch() {
        return latch;
    }

    public void resetLatch() {
        latch = new CountDownLatch(steps);
    }
}
