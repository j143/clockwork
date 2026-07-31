package com.j143.clockwork.store;

import com.j143.clockwork.core.Job;
import com.j143.clockwork.core.JobStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository {
    void insert(Job job);

    List<Job> claimDueJobs(int limit);

    void updateStatus(UUID id, JobStatus status);

    Optional<Job> findById(UUID id);
}
