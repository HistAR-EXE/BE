package com.histar.be.group.dto;

import java.util.List;
import java.util.UUID;

public record GroupDetailResponse(UUID id, String name, String code, List<GroupMemberResponse> members) {}
