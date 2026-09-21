package com.yupi.springbootinit.airuntime.exception;

public class AiRuntimeException extends RuntimeException {
    private final AiRuntimeErrorCode errorCode;
    private final boolean retryable;
    private final Integer httpStatus;
    private final String upstreamRequestId;
    private final String sanitizedMessage;
    private final String traceId;

    public AiRuntimeException(AiRuntimeErrorCode errorCode, boolean retryable, String message) {
        this(errorCode, retryable, null, null, message, null, null);
    }

    public AiRuntimeException(AiRuntimeErrorCode errorCode, boolean retryable, Integer httpStatus,
                              String upstreamRequestId, String sanitizedMessage, String traceId, Throwable cause) {
        super(sanitizedMessage, cause);
        this.errorCode = errorCode;
        this.retryable = retryable;
        this.httpStatus = httpStatus;
        this.upstreamRequestId = upstreamRequestId;
        this.sanitizedMessage = sanitizedMessage;
        this.traceId = traceId;
    }

    public AiRuntimeErrorCode getErrorCode() { return errorCode; }
    public boolean isRetryable() { return retryable; }
    public Integer getHttpStatus() { return httpStatus; }
    public String getUpstreamRequestId() { return upstreamRequestId; }
    public String getSanitizedMessage() { return sanitizedMessage; }
    public String getTraceId() { return traceId; }
}
