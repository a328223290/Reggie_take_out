package com.itheima.reggie.common;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 元数据对象处理器，用于自动填充数据
 */
@Slf4j
@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    /**
     * 插入数据时自动填充数据的逻辑
     * @param metaObject
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        log.info("INSERT自动填充字段...");
        metaObject.setValue("createTime", LocalDateTime.now());
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("createUser", BaseContext.getCurId());
        metaObject.setValue("updateUser", BaseContext.getCurId());

    }

    /**
     * 更新数据时自动填充数据的逻辑
     * @param metaObject
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        log.info("UPDATE自动填充字段...");
        metaObject.setValue("updateTime", LocalDateTime.now());
        metaObject.setValue("updateUser", BaseContext.getCurId());
    }
}
