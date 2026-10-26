package com.histar.be.admin.dto;

import java.time.Instant;

public record DemoActivitySnapshot(int activeUsers, long windowSeconds, Instant checkedAt) {}
