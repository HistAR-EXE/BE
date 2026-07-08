package com.histar.be.common.exception;

import lombok.Getter;

@Getter
public class QuotaExceededException extends BaseException {

    private final String upgradeUrl;
    private final String quotaType;
    private final String upgradePackage;

    public QuotaExceededException(String message, String upgradeUrl) {
        this(message, upgradeUrl, "B2C_DAILY", null);
    }

    public QuotaExceededException(String message, String upgradeUrl, String quotaType, String upgradePackage) {
        super(ErrorCode.QUOTA_EXCEEDED, message);
        this.upgradeUrl = upgradeUrl;
        this.quotaType = quotaType;
        this.upgradePackage = upgradePackage;
    }
}
