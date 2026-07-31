package com.j143.clockwork.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.UUID;

public record Job(
        UUID id,
        String clientId,
        String callbackUrl,
        JsonNode payload,
        Instant scheduledAt,
        JobStatus status,
        Instant createdAt
) {
}
