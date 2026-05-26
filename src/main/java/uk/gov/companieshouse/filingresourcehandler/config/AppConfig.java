package uk.gov.companieshouse.filingresourcehandler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

import java.util.function.Supplier;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Value("${timeout.milliseconds}")
    private int timeoutMilliseconds;

    @Value("${api.local.url}")
    private String apiLocalUrl;

    @Bean
    Supplier<InternalApiClient> internalApiClientSupplier() {
        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
            internalApiClient.setBasePath(apiLocalUrl);
            return internalApiClient;
        };
    }


    @Bean
    public RestClient restClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMilliseconds);
        requestFactory.setReadTimeout(timeoutMilliseconds);
        return RestClient.builder().baseUrl(apiLocalUrl)
                .requestFactory(requestFactory)
                .defaultHeader("Authorization", internalApiKey)
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
