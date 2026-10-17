package com.histar.be.admin.dto;

import java.util.List;

public record OrgCertificateBulkResult(int issued, List<OrgCertificateResponse> certificates) {}
