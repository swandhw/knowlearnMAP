package com.knowlearnmap.common.exception;

/**
 * 동기화가 필요한 상태에서 기능 사용 시 발생하는 예외
 */
public class SyncRequiredException extends RuntimeException {

    public SyncRequiredException(String message) {
        super(message);
    }

    public SyncRequiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
