package com.histar.be.common.exception;

public class AuthException extends BaseException {

    public AuthException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
