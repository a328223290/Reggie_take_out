package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLIntegrityConstraintViolationException;

/*
 * 全局异常处理
 */
// @ControllerAdvice是 Spring MVC 提供的一个注解，用于定义全局控制器增强（Controller Advice）。它允许在多个控制器中共享相同的异常处理逻辑、模型数据、或其他通用的控制器增强行为。
@ControllerAdvice(annotations = {RestController.class, Controller.class})
// @ResponseBody是 Spring 框架中的一个注解，用于指示方法的返回值直接作为响应体返回给客户端，而不是通过视图解析器进行渲染。这通常用于构建 RESTful 服务或返回 JSON 数据。
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 全局处理元素已经存在的错误
     * @param e
     * @return
     */
    @ExceptionHandler({SQLIntegrityConstraintViolationException.class})
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException e){
        log.info("出现异常{}", e.getMessage());
        return R.error("员工已存在");
    }

    /**
     * 全局处理自定义业务错误
     * @param e
     * @return
     */
    @ExceptionHandler({CustomException.class})
    public R<String> exceptionHandler(CustomException e){
        log.info("出现异常{}", e.getMessage());
        return R.error(e.getMessage());
    }
}
