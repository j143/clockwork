package com.j143.clockwork.server.scheduler;

import com.j143.clockwork.core.Job;
import com.j143.clockwork.core.JobStatus;
import com.j143.clockwork.store.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class JobExecutor {
    private static final Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private final JobRepository jobRepository;
    private final RestClient restClient;
    private final BlockingQueue<Job> queue;
    private final ExecutorService workers = Executors.newFixedThreadPool(4);
    private volatile boolean running = true;

    public JobExecutor(JobRepository jobRepository, RestClient restClient, BlockingQueue<Job> queue) {
        this.jobRepository = jobRepository;
        this.restClient = restClient;
        this.queue = queue;
    }

    @PostConstruct
    void startWorkers() {
        for (int i = 0; i < 4; i++) {
            workers.submit(this::runWorker);
        }
    }

    @PreDestroy
    void stopWorkers() throws InterruptedException {
        running = false;
        workers.shutdownNow();
        workers.awaitTermination(5, TimeUnit.SECONDS);
    }

    private void runWorker() {
        while (running) {
            try {
                Job job = queue.take();
                dispatch(job);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void dispatch(Job job) {
        try {
            restClient.post()
                    .uri(job.callbackUrl())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(job.payload())
                    .retrieve()
                    .toBodilessEntity();
            jobRepository.updateStatus(job.id(), JobStatus.DONE);
        } catch (Exception e) {
            log.warn("callback failed for job {}: {}", job.id(), e.getMessage());
            jobRepository.updateStatus(job.id(), JobStatus.PENDING);
        }
    }
}
