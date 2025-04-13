package com.xiaou.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class MyMetaObjectHandler implements MetaObjectHandler {

    public MyMetaObjectHandler() {
        System.out.println("===> MyMetaObjectHandler 被 Spring 容器创建了！");
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        this.strictInsertFill(metaObject, "createBy",String.class,"xiaou61");
        this.strictInsertFill(metaObject, "createTime", Date.class,new Date());
        this.strictInsertFill(metaObject, "deleteFlag",Integer.class,0);
        this.strictInsertFill(metaObject, "version",Integer.class,0);

    }

    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateBy",String.class,"xiaou61");
        this.strictUpdateFill(metaObject, "updateTime", Date.class,new Date());
    }
}
