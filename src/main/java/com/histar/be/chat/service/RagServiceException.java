package com.histar.be.chat.service;

import com.histar.be.common.exception.BaseException;
import com.histar.be.common.exception.ErrorCode;
import java.util.Map;

/** Public RAG failure contract shared by synchronous HTTP and SSE. */
public class RagServiceException extends BaseException {
    private final boolean retryable;
    private final String requestId;

    public RagServiceException(boolean timeout, boolean retryable, String requestId) {
        super(timeout ? ErrorCode.RAG_TIMEOUT : ErrorCode.RAG_UNAVAILABLE,
                timeout ? "Dịch vụ trả lời đang mất quá nhiều thời gian. Vui lòng thử lại."
                        : "Dịch vụ trả lời hiện không khả dụng. Vui lòng thử lại.");
        this.retryable = retryable;
        this.requestId = requestId == null || requestId.isBlank() ? java.util.UUID.randomUUID().toString() : requestId;
    }

    public boolean isRetryable() { return retryable; }
    public String getRequestId() { return requestId; }

    public Map<String, Object> payload() {
        return Map.of("code", getErrorCode().getCode(), "message", getMessage(),
                "retryable", retryable, "requestId", requestId);
    }
}
