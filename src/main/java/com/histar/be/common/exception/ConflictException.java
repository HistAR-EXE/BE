package com.histar.be.common.exception;

public class ConflictException extends BaseException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }
}
