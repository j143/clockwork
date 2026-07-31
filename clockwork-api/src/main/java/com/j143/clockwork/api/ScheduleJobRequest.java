package com.j143.clockwork.api;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record ScheduleJobRequest(
        String clientId,
        String callbackUrl,
        JsonNode payload,
        Instant scheduledAt
) {
}
