package com.j143.clockwork.server.web;

import com.j143.clockwork.api.JobResponse;
import com.j143.clockwork.api.ScheduleJobRequest;
import com.j143.clockwork.core.Clock;
import com.j143.clockwork.core.Job;
import com.j143.clockwork.core.JobStatus;
import com.j143.clockwork.store.JobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/jobs")
public class JobController {
    private final JobRepository jobRepository;
    private final Clock clock;

    public JobController(JobRepository jobRepository, Clock clock) {
        this.jobRepository = jobRepository;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<JobResponse> schedule(@RequestBody ScheduleJobRequest request) {
        validate(request);
        UUID id = UUID.randomUUID();
        jobRepository.insert(new Job(
                id,
                request.clientId(),
                request.callbackUrl(),
                request.payload(),
                request.scheduledAt(),
                JobStatus.PENDING,
                clock.now()
        ));
        return ResponseEntity.accepted().body(new JobResponse(id));
    }

    private void validate(ScheduleJobRequest request) {
        if (request == null || request.clientId() == null || request.clientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "clientId is required");
        }
        if (request.callbackUrl() == null || request.callbackUrl().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "callbackUrl is required");
        }
        URI uri;
        try {
            uri = URI.create(request.callbackUrl());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "callbackUrl must be a valid URL", e);
        }
        if (uri.getScheme() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "callbackUrl must use http or https");
        }
        if (request.scheduledAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "scheduledAt is required");
        }
    }
}
