package com.example.lab5.exception;

/** Người dùng đã đăng nhập nhưng không có quyền truy cập tài nguyên của người khác. */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
