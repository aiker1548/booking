package com.hotelbooking.booking.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${INTERNAL_API_SECRET:}") String internalApiSecret
    ) {
        RestTemplate restTemplate = new RestTemplate();

        ClientHttpRequestInterceptor internalSecretInterceptor =
                (request, body, execution) -> {
                    request.getHeaders().set(
                            "X-Internal-Secret",
                            internalApiSecret
                    );
                    return execution.execute(request, body);
                };

        restTemplate.getInterceptors().add(internalSecretInterceptor);

        return restTemplate;
    }
}