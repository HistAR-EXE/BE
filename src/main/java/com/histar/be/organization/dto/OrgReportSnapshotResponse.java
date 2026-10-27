package com.histar.be.organization.dto;

import com.histar.be.liveboard.dto.LiveBoardSnapshot;
import java.time.Instant;
import java.util.UUID;

public record OrgReportSnapshotResponse(UUID id, Instant createdAt, LiveBoardSnapshot snapshot) {}
