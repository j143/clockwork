package com.j143.clockwork.core;

import java.time.Instant;

@FunctionalInterface
public interface Clock {
    Instant now();
}
