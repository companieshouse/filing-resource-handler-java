package uk.gov.companieshouse.filingresourcehandler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {

    public static final String NAMESPACE = "filing-resource-handler-java";

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
