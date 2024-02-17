package com.itheima.reggie.common;

/**
 * 自定义业务错误
 */
public class CustomException extends RuntimeException{
    public CustomException(String msg){
        super(msg);
    }
}
