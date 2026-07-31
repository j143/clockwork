package com.j143.clockwork.server.scheduler;

import com.j143.clockwork.core.Job;
import com.j143.clockwork.core.JobStatus;
import com.j143.clockwork.store.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.BlockingQueue;

@Component
public class JobPoller {
    private static final Logger log = LoggerFactory.getLogger(JobPoller.class);

    private final JobRepository jobRepository;
    private final BlockingQueue<Job> queue;

    public JobPoller(JobRepository jobRepository, BlockingQueue<Job> queue) {
        this.jobRepository = jobRepository;
        this.queue = queue;
    }

    @Scheduled(fixedDelay = 1000)
    public void poll() {
        List<Job> due = jobRepository.claimDueJobs(100);
        for (Job job : due) {
            boolean enqueued = queue.offer(job);
            if (!enqueued) {
                log.warn("queue full; requeueing job {}", job.id());
                jobRepository.updateStatus(job.id(), JobStatus.PENDING);
            }
        }
    }
}
