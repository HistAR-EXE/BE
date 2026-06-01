package com.histar.be.common.exception;

public class BusinessRuleException extends BaseException {

    public BusinessRuleException(String message) {
        super(ErrorCode.BUSINESS_RULE, message);
    }
}
