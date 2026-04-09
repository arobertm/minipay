package ro.minipay.vault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    RestTemplate restTemplate(RestTemplateBuilder builder,
                              @Value("${minids.connect-timeout:5000}") int connectTimeout,
                              @Value("${minids.read-timeout:10000}") int readTimeout) {
        return builder
            .connectTimeout(Duration.ofMillis(connectTimeout))
            .readTimeout(Duration.ofMillis(readTimeout))
            .build();
    }
}
