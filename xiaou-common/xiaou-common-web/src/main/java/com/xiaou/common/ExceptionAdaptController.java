package com.xiaou.common;

import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExceptionAdaptController {

    @ExceptionHandler(RuntimeException.class)
    private R runTimeException(RuntimeException runtimeException) {
        runtimeException.printStackTrace();
        return R.fail("系统异常");
    }

    @ExceptionHandler(Exception.class)
    private R runTimeException(Exception exception) {
        exception.printStackTrace();
        return R.fail("系统异常");
    }
}
