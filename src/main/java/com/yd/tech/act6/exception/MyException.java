package com.yd.tech.act6.exception;

/**
 * 自定义异常
 *
 * @author pxy
 */
public class MyException extends RuntimeException{

    private static final long serialVersionUID = 1L;

    /**
     * 提供无参数的构造方法
     */
    public MyException() {
    }

    /**
     * 提供一个有参数的构造方法，可自动生成
     * @param message
     */
    public MyException(String message) {
        // 把参数传递给Throwable的带String参数的构造方法
        super(message);
    }

}
