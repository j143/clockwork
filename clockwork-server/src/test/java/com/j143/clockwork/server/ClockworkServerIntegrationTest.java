package com.j143.clockwork.server;

import com.j143.clockwork.api.JobResponse;
import com.j143.clockwork.api.ScheduleJobRequest;
import com.j143.clockwork.core.JobStatus;
import com.j143.clockwork.store.JobRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ClockworkServerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JobRepository jobRepository;

    @LocalServerPort
    private int port;

    private HttpServer callbackStub;
    private CountDownLatch callbackLatch;
    private AtomicInteger callbackCount;

    @BeforeEach
    void setUp() throws IOException {
        callbackLatch = new CountDownLatch(1);
        callbackCount = new AtomicInteger(0);
        callbackStub = HttpServer.create(new InetSocketAddress(0), 0);
        callbackStub.createContext("/callback", exchange -> {
            callbackCount.incrementAndGet();
            callbackLatch.countDown();
            exchange.sendResponseHeaders(200, 0);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write("ok".getBytes());
            }
        });
        callbackStub.start();
    }

    @AfterEach
    void tearDown() {
        callbackStub.stop(0);
    }

    @Test
    void schedulesAndExecutesJobEndToEnd() throws InterruptedException {
        String callbackUrl = "http://localhost:" + callbackStub.getAddress().getPort() + "/callback";
        ScheduleJobRequest request = new ScheduleJobRequest(
                "demo-client",
                callbackUrl,
                null,
                Instant.now().plusSeconds(2)
        );

        ResponseEntity<JobResponse> response = restTemplate.exchange(
                "http://localhost:" + port + "/jobs",
                HttpMethod.POST,
                new HttpEntity<>(request),
                JobResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        JobResponse body = response.getBody();
        assertThat(body).isNotNull();
        UUID jobId = body.jobId();

        assertThat(callbackLatch.await(10, TimeUnit.SECONDS)).isTrue();
        assertThat(callbackCount.get()).isEqualTo(1);

        JobStatus finalStatus = awaitStatus(jobId, JobStatus.DONE, Duration.ofSeconds(10));
        assertThat(finalStatus).isEqualTo(JobStatus.DONE);
    }

    private JobStatus awaitStatus(UUID jobId, JobStatus expected, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            JobStatus status = jobRepository.findById(jobId).map(job -> job.status()).orElse(null);
            if (status == expected) {
                return status;
            }
            Thread.sleep(200);
        }
        return jobRepository.findById(jobId).map(job -> job.status()).orElse(null);
    }
}
