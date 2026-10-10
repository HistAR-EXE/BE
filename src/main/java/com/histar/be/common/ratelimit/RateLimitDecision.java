package com.histar.be.common.ratelimit;

public record RateLimitDecision(boolean allowed, long remaining, long retryAfterSeconds) {}
