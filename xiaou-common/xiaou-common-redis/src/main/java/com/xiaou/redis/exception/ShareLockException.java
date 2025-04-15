package com.xiaou.redis.exception;

public class ShareLockException extends RuntimeException{
    public ShareLockException(String message) {
        super(message);
    }
}
