package com.histar.be.admin.dto;

import java.util.List;

public record OrgArCardIssueResult(int issued, int skipped, List<OrgArCardResponse> cards) {}
