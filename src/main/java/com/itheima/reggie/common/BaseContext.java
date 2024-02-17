package com.itheima.reggie.common;

/**
 * 用于记录当前employee id的类。
 * 在自动填充字段的时候需要获取当前的employee id，而在定义MetaObjectHandler的时候没办法通过request获取id，因此利用处理请求的逻辑同在一个线程下的特性将id存放在ThreadLocal中以传递信息。
 */
public class BaseContext {
    private static ThreadLocal<Long> threadLocal = new ThreadLocal<>();

    public static void setId(Long val){
        threadLocal.set(val);
    }

    public static Long getCurId(){
        return threadLocal.get();
    }
}
