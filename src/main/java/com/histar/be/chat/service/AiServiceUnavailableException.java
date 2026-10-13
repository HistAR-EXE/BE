package com.histar.be.chat.service;

import com.histar.be.common.exception.BusinessRuleException;

public class AiServiceUnavailableException extends BusinessRuleException {

    public AiServiceUnavailableException(String dependency, int attempts, Throwable cause) {
        super("Dịch vụ AI " + dependency + " không khả dụng sau " + attempts + " lần thử: "
                + (cause instanceof java.util.concurrent.TimeoutException ? "timed out" : cause.getMessage()));
        initCause(cause);
    }
}
