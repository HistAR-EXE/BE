package com.histar.be.organization.dto;

public record OrgCapabilities(
        boolean manageMembers, boolean manageTeachers, boolean assignments, boolean liveBoard,
        boolean reports, boolean fieldTrips, boolean contentDrafts,
        boolean organizationActive, boolean premiumAccess) {}
