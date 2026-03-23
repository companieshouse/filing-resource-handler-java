package uk.gov.companieshouse.filingresourcehandler.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

import java.time.Duration;
import java.util.function.Supplier;

@Configuration
public class AppConfig {

    @Value("${internal.api-key}")
    private String internalApiKey;

    @Value("${timeout.milliseconds}")
    private int timeoutMilliseconds;

    @Value("${api.local.url}")
    private String apiLocalUrl;

    @Value("${internal.api-url}")
    private String apiUrl;

    @Bean
    Supplier<InternalApiClient> internalApiClientSupplier() {
        return () -> {
            InternalApiClient internalApiClient = new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
            internalApiClient.setBasePath(apiUrl);
            return internalApiClient;
        };
    }

    @Bean
    public WebClient webClient() {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofMillis(timeoutMilliseconds));
        return WebClient.builder()
                .defaultHeaders(headers ->
                        headers.setBasicAuth(internalApiKey, "")
                ).baseUrl(apiLocalUrl).clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
