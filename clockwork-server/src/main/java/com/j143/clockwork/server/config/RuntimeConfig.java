package com.j143.clockwork.server.config;

import com.j143.clockwork.core.Clock;
import com.j143.clockwork.core.Job;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Configuration
public class RuntimeConfig {

    @Bean
    public Clock clock() {
        return Instant::now;
    }

    @Bean
    public BlockingQueue<Job> jobQueue() {
        return new LinkedBlockingQueue<>(1_000);
    }

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(requestFactory).build();
    }
}
