package com.parcellocker.dpc.api;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String title;
    private final Integer retryAfterSeconds;

    public ApiException(HttpStatus status, String title, String detail) {
        this(status, title, detail, null);
    }

    public ApiException(HttpStatus status, String title, String detail, Integer retryAfterSeconds) {
        super(detail);
        this.status = status;
        this.title = title;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}