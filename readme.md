## mybatis元数据的应用

就是一个拦截器。

```java
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
```

可以方便我们在insert 和update的时候，添加一些固定的参数。

需要注意的就是要在实体类上添加注解

```java
package com.xiaou.web.entity.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.util.Date;

@TableName("user")
@Data
public class UserPo {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer age;

    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer deleteFlag;

    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}

```

## druid图形化监控

就是一个配置

```yml
type: com.alibaba.druid.pool.DruidDataSource
druid:
  # 初始化大小
  initial-size: 20
  # 最小连接池数量
  min-idle: 20
  # 最大连接池数量
  max-active: 200
  # 配置获取连接等待超时的时间
  max-wait: 60000
  stat-view-servlet:
    enabled: true
    url-pattern: /druid/*
    login-username: admin
    login-password: 123456
  filter:
    stat:
      enabled: true
      log-slow-sql: true
      slow-sql-millis: 2000
```



![image-20250414140621219](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141406446.png)

还有一些防火墙之类的

```yml
wall:
  enabled: true
```



用这个配置进行配置就可以了

## mybatis-plus优化器

这里就是固定的

```java
package com.xiaou.interceptor;

import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.plugin.*;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.defaults.DefaultSqlSession.StrictMap;

import java.lang.reflect.Field;
import java.sql.Statement;
import java.util.*;

@Intercepts(value = {
        @Signature(args = {Statement.class, ResultHandler.class}, method = "query", type = StatementHandler.class),
        @Signature(args = {Statement.class}, method = "update", type = StatementHandler.class),
        @Signature(args = {Statement.class}, method = "batch", type = StatementHandler.class)})
public class SqlBeautyInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object target = invocation.getTarget();
        long startTime = System.currentTimeMillis();
        StatementHandler statementHandler = (StatementHandler) target;
        try {
            return invocation.proceed();
        } finally {
            long endTime = System.currentTimeMillis();
            long sqlCost = endTime - startTime;
            BoundSql boundSql = statementHandler.getBoundSql();
            String sql = boundSql.getSql();
            Object parameterObject = boundSql.getParameterObject();
            List<ParameterMapping> parameterMappingList = boundSql.getParameterMappings();
            sql = formatSql(sql, parameterObject, parameterMappingList);
            System.out.println("SQL： [ " + sql + " ]执行耗时[ " + sqlCost + "ms ]");
        }
    }

    @Override
    public Object plugin(Object o) {
        return Plugin.wrap(o, this);
    }

    @Override
    public void setProperties(Properties properties) {
    }

    private String formatSql(String sql, Object parameterObject, List<ParameterMapping> parameterMappingList) {
        if (sql == "" || sql.length() == 0) {
            return "";
        }
        sql = beautifySql(sql);
        if (parameterObject == null || parameterMappingList == null || parameterMappingList.size() == 0) {
            return sql;
        }
        String sqlWithoutReplacePlaceholder = sql;
        try {
            if (parameterMappingList != null) {
                Class<?> parameterObjectClass = parameterObject.getClass();
                if (isStrictMap(parameterObjectClass)) {
                    StrictMap<Collection<?>> strictMap = (StrictMap<Collection<?>>) parameterObject;
                    if (isList(strictMap.get("list").getClass())) {
                        sql = handleListParameter(sql, strictMap.get("list"));
                    }
                } else if (isMap(parameterObjectClass)) {
                    Map<?, ?> paramMap = (Map<?, ?>) parameterObject;
                    sql = handleMapParameter(sql, paramMap, parameterMappingList);
                } else {
                    sql = handleCommonParameter(sql, parameterMappingList, parameterObjectClass, parameterObject);
                }
            }
        } catch (Exception e) {
            return sqlWithoutReplacePlaceholder;
        }
        return sql;
    }


    private String handleCommonParameter(String sql, List<ParameterMapping> parameterMappingList,
                                         Class<?> parameterObjectClass, Object parameterObject) throws Exception {
        Class<?> originalParameterObjectClass = parameterObjectClass;
        List<Field> allFieldList = new ArrayList<>();
        while (parameterObjectClass != null) {
            allFieldList.addAll(new ArrayList<>(Arrays.asList(parameterObjectClass.getDeclaredFields())));
            parameterObjectClass = parameterObjectClass.getSuperclass();
        }
        Field[] fields = new Field[allFieldList.size()];
        fields = allFieldList.toArray(fields);
        parameterObjectClass = originalParameterObjectClass;
        for (ParameterMapping parameterMapping : parameterMappingList) {
            String propertyValue = null;
            if (isPrimitiveOrPrimitiveWrapper(parameterObjectClass)) {
                propertyValue = parameterObject.toString();
            } else {
                String propertyName = parameterMapping.getProperty();
                Field field = null;
                for (Field everyField : fields) {
                    if (everyField.getName().equals(propertyName)) {
                        field = everyField;
                    }
                }
                field.setAccessible(true);
                propertyValue = String.valueOf(field.get(parameterObject));
                if (parameterMapping.getJavaType().isAssignableFrom(String.class)) {
                    propertyValue = "\"" + propertyValue + "\"";
                }
            }
            sql = sql.replaceFirst("\\?", propertyValue);
        }
        return sql;
    }

    private String handleMapParameter(String sql, Map<?, ?> paramMap, List<ParameterMapping> parameterMappingList) {
        for (ParameterMapping parameterMapping : parameterMappingList) {
            Object propertyName = parameterMapping.getProperty();
            Object propertyValue = paramMap.get(propertyName);
            if (propertyValue != null) {
                if (propertyValue.getClass().isAssignableFrom(String.class)) {
                    propertyValue = "\"" + propertyValue + "\"";
                }

                sql = sql.replaceFirst("\\?", propertyValue.toString());
            }
        }
        return sql;
    }

    private String handleListParameter(String sql, Collection<?> col) {
        if (col != null && col.size() != 0) {
            for (Object obj : col) {
                String value = null;
                Class<?> objClass = obj.getClass();
                if (isPrimitiveOrPrimitiveWrapper(objClass)) {
                    value = obj.toString();
                } else if (objClass.isAssignableFrom(String.class)) {
                    value = "\"" + obj.toString() + "\"";
                }

                sql = sql.replaceFirst("\\?", value);
            }
        }
        return sql;
    }

    private String beautifySql(String sql) {
        sql = sql.replaceAll("[\\s\n ]+", " ");
        return sql;
    }

    private boolean isPrimitiveOrPrimitiveWrapper(Class<?> parameterObjectClass) {
        return parameterObjectClass.isPrimitive() || (parameterObjectClass.isAssignableFrom(Byte.class)
                || parameterObjectClass.isAssignableFrom(Short.class)
                || parameterObjectClass.isAssignableFrom(Integer.class)
                || parameterObjectClass.isAssignableFrom(Long.class)
                || parameterObjectClass.isAssignableFrom(Double.class)
                || parameterObjectClass.isAssignableFrom(Float.class)
                || parameterObjectClass.isAssignableFrom(Character.class)
                || parameterObjectClass.isAssignableFrom(Boolean.class));
    }

    /**
     * 是否DefaultSqlSession的内部类StrictMap
     */
    private boolean isStrictMap(Class<?> parameterObjectClass) {
        return parameterObjectClass.isAssignableFrom(StrictMap.class);
    }

    /**
     * 是否List的实现类
     */
    private boolean isList(Class<?> clazz) {
        Class<?>[] interfaceClasses = clazz.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(List.class)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 是否Map的实现类
     */
    private boolean isMap(Class<?> parameterObjectClass) {
        Class<?>[] interfaceClasses = parameterObjectClass.getInterfaces();
        for (Class<?> interfaceClass : interfaceClasses) {
            if (interfaceClass.isAssignableFrom(Map.class)) {
                return true;
            }
        }
        return false;
    }

}
```

之后是config

```java
package com.xiaou.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;

import com.xiaou.interceptor.SqlBeautyInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisConfiguration {

    @Bean
    @ConditionalOnProperty(name = {"sql.beauty.show"}, havingValue = "true", matchIfMissing = true)
    public SqlBeautyInterceptor sqlBeautyInterceptor() {
        return new SqlBeautyInterceptor();
    }

    @Bean
    public MybatisPlusInterceptor MybatisPlusInterceptor() {
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        mybatisPlusInterceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return mybatisPlusInterceptor;
    }

}
```

效果就是这样的：

![image-20250414141356913](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141413977.png)

当然关于这个，如果说是测试项目可以用p6spy 

实际上线的话还是最好用这种方式。

因为p6spy 占用率比较高。

之后我们的这个插件，我们需要配置一个配置，让他们动态的加载。

就是config里面的

```java
@ConditionalOnProperty(name = {"sql.beauty.show"}, havingValue = "true", matchIfMissing = true)
```

之后去applcation.yml里面加上这个就可以了。

## mybatis公共字段抽取

就是对一些createtime，updatetime等进行一个公共的抽取。

```java
package com.xiaou.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
public class BaseEntity implements Serializable {
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(fill = FieldFill.UPDATE)
    private Date updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Integer deleteFlag;

    @TableField(fill = FieldFill.INSERT)
    private Integer version;
}
```

## 通用结果封装



这个没什么好说的

```java
package com.xiaou;


import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 响应信息主体
 *
 * @author Lion Li
 */
@Data
@NoArgsConstructor
public class R<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成功
     */
    public static final int SUCCESS = 200;

    /**
     * 失败
     */
    public static final int FAIL = 500;

    private int code;

    private String msg;

    private T data;

    public static <T> R<T> ok() {
        return restResult(null, SUCCESS, "操作成功");
    }

    public static <T> R<T> ok(T data) {
        return restResult(data, SUCCESS, "操作成功");
    }

    public static <T> R<T> ok(String msg) {
        return restResult(null, SUCCESS, msg);
    }

    public static <T> R<T> ok(String msg, T data) {
        return restResult(data, SUCCESS, msg);
    }

    public static <T> R<T> fail() {
        return restResult(null, FAIL, "操作失败");
    }

    public static <T> R<T> fail(String msg) {
        return restResult(null, FAIL, msg);
    }

    public static <T> R<T> fail(T data) {
        return restResult(data, FAIL, "操作失败");
    }

    public static <T> R<T> fail(String msg, T data) {
        return restResult(data, FAIL, msg);
    }

    public static <T> R<T> fail(int code, String msg) {
        return restResult(null, code, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @return 警告消息
     */
    public static <T> R<T> warn(String msg) {
        return restResult(null, 601, msg);
    }

    /**
     * 返回警告消息
     *
     * @param msg 返回内容
     * @param data 数据对象
     * @return 警告消息
     */
    public static <T> R<T> warn(String msg, T data) {
        return restResult(data, 601, msg);
    }

    private static <T> R<T> restResult(T data, int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setData(data);
        r.setMsg(msg);
        return r;
    }

    public static <T> Boolean isError(R<T> ret) {
        return !isSuccess(ret);
    }

    public static <T> Boolean isSuccess(R<T> ret) {
        return R.SUCCESS == ret.getCode();
    }
}
```

有了之后就是

改写之前的方法的话

```java
@PostMapping
public R<Integer> insert(@RequestBody UserReq userReq){
    UserDto userDto = new UserDto();
    BeanUtils.copyProperties(userReq,userDto);
    int i = userService.addUser(userDto);
    return R.ok(i);
}
```

更加方面前端的调用

![image-20250414143330578](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141433685.png)

## 全局异常统一封装

这里用到了RestControllerAdvice跟ExceptionHandler



```java
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
```

## 逻辑删除拦截器

就是一个配置

```yml
mybatis-plus:
  global-config:
    db-config:
      # 逻辑删除配置
      logic-delete-field: delete_flag
      logic-delete-value: 1
      logic-not-delete-value: 0
```



之后添加注释就可以了

```java
@TableField(fill = FieldFill.INSERT)
@TableLogic
private Integer deleteFlag;
```

之后我们测试一下

![image-20250414144902850](C:/Users/Lenovo/AppData/Roaming/Typora/typora-user-images/image-20250414144902850.png)

![image-20250414144917265](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141449325.png)

## 分页拦截器集分页统一封装

首先我们要创建两个结果实体对象。

第一个是前端传过来的数据

```java
public class PageReqDto {

    /**
     * 请求页码，默认第 1 页
     */
    private int pageNum = 1;

    /**
     * 每页大小，默认每页 10 条
     */
    private int pageSize = 10;

    /**
     * 是否查询所有，默认不查所有 为 true 时，pageNum 和 pageSize 无效
     */
    private boolean fetchAll = false;

}
```

就是需要一个这个就可以了。

之后我们返回给前端的数据：

```java
package com.xiaou.entity;

import java.util.List;
import lombok.Getter;

/**
 * 分页响应数据格式封装
 */
@Getter
public class
PageRespDto<T> {

    /**
     * 页码
     */
    private final long pageNum;

    /**
     * 每页大小
     */
    private final long pageSize;

    /**
     * 总记录数
     */
    private final long total;

    /**
     * 分页数据集
     */
    private final List<? extends T> list;

    /**
     * 该构造函数用于通用分页查询的场景 接收普通分页数据和普通集合
     */
    public PageRespDto(long pageNum, long pageSize, long total, List<T> list) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.total = total;
        this.list = list;
    }

    public static <T> PageRespDto<T> of(long pageNum, long pageSize, long total, List<T> list) {
        return new PageRespDto<>(pageNum, pageSize, total, list);
    }

    /**
     * 获取分页数
     */
    public long getPages() {
        if (this.pageSize == 0L) {
            return 0L;
        } else {
            long pages = this.total / this.pageSize;
            if (this.total % this.pageSize != 0L) {
                ++pages;
            }
            return pages;
        }
    }
}
```

然后是一个简单的实现方式

一般情况下就是，首先创建Myabtis-plus提供的IPage

把查询的条件塞进去。之后用selectPage进行查询。

之后返回所需要封装结果返回类就可以了。

```java
@Override
public PageRespDto<UserPo> getUserPage(PageReqDto pageReqDto) {
    IPage<UserPo> page = new Page<>();
    page.setCurrent(pageReqDto.getPageNum());
    page.setSize(pageReqDto.getPageSize());
    QueryWrapper<UserPo> queryWrapper = new QueryWrapper<>();
    queryWrapper.eq("delete_flag", 0);
    IPage<UserPo> userPoIPage = userMapper.selectPage(page, queryWrapper);
    return new PageRespDto<>(pageReqDto.getPageNum(), pageReqDto.getPageSize(), userPoIPage.getTotal(), userPoIPage.getRecords());
}
```

![image-20250414164219202](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141642417.png)

## 代码生成器

这个需要根据自己的业务可以自己去实际开发。

```java
package com.xiaou.tkhai.generate;

import cn.hutool.core.io.FileUtil;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.io.FileWriter;
import java.io.Writer;

/**
 * 代码生成器
 *
 *
 * @from 
 */
public class CodeGenerator {

    /**
     * 用法：修改生成参数和生成路径，注释掉不需要的生成逻辑，然后运行即可
     *
     * @param args
     * @throws TemplateException
     * @throws IOException
     */
    public static void main(String[] args) throws TemplateException, IOException {
        // 指定生成参数
        String packageName = "com.xiaou.tkhai";
        // 数据表中文名称
        String dataName = "题库题目关联";
        // 数据库表名
        String dataKey = "questionBankQuestion";
        // 数据库表名首字母大写
        String upperDataKey = "QuestionBankQuestion";

        // 封装生成参数
        Map<String, Object> dataModel = new HashMap<>();
        dataModel.put("packageName", packageName);
        dataModel.put("dataName", dataName);
        dataModel.put("dataKey", dataKey);
        dataModel.put("upperDataKey", upperDataKey);

        // 生成路径默认值
        String projectPath = System.getProperty("user.dir");
        // 参考路径，可以自己调整下面的 outputPath
        String inputPath = projectPath + File.separator + "src/main/resources/templates/模板名称.java.ftl";
        String outputPath = String.format("%s/generator/包名/%s类后缀.java", projectPath, upperDataKey);

        // 1、生成 Controller
        // 指定生成路径
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateController.java.ftl";
        outputPath = String.format("%s/generator/controller/%sController.java", projectPath, upperDataKey);
        // 生成
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("生成 Controller 成功，文件路径：" + outputPath);

        // 2、生成 Service 接口和实现类
        // 生成 Service 接口
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateService.java.ftl";
        outputPath = String.format("%s/generator/service/%sService.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("生成 Service 接口成功，文件路径：" + outputPath);
        // 生成 Service 实现类
        inputPath = projectPath + File.separator + "src/main/resources/templates/TemplateServiceImpl.java.ftl";
        outputPath = String.format("%s/generator/service/impl/%sServiceImpl.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("生成 Service 实现类成功，文件路径：" + outputPath);

        // 3、生成数据模型封装类（包括 DTO 和 VO）
        // 生成 DTO
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateAddRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sAddRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateQueryRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sQueryRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateEditRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sEditRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateUpdateRequest.java.ftl";
        outputPath = String.format("%s/generator/model/dto/%sUpdateRequest.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("生成 DTO 成功，文件路径：" + outputPath);
        // 生成 VO
        inputPath = projectPath + File.separator + "src/main/resources/templates/model/TemplateVO.java.ftl";
        outputPath = String.format("%s/generator/model/vo/%sVO.java", projectPath, upperDataKey);
        doGenerate(inputPath, outputPath, dataModel);
        System.out.println("生成 VO 成功，文件路径：" + outputPath);
    }

    /**
     * 生成文件
     *
     * @param inputPath  模板文件输入路径
     * @param outputPath 输出路径
     * @param model      数据模型
     * @throws IOException
     * @throws TemplateException
     */
    public static void doGenerate(String inputPath, String outputPath, Object model) throws IOException, TemplateException {
        // new 出 Configuration 对象，参数为 FreeMarker 版本号
        Configuration configuration = new Configuration(Configuration.VERSION_2_3_31);

        // 指定模板文件所在的路径
        File templateDir = new File(inputPath).getParentFile();
        configuration.setDirectoryForTemplateLoading(templateDir);

        // 设置模板文件使用的字符集
        configuration.setDefaultEncoding("utf-8");

        // 创建模板对象，加载指定模板
        String templateName = new File(inputPath).getName();
        Template template = configuration.getTemplate(templateName);

        // 文件不存在则创建文件和父目录
        if (!FileUtil.exist(outputPath)) {
            FileUtil.touch(outputPath);
        }

        // 生成
        Writer out = new FileWriter(outputPath);
        template.process(model, out);

        // 生成文件后别忘了关闭哦
        out.close();
    }
}
```

之后写一些ftl的模板就可以了。

## mapstruct属性拷贝

因为Beanutils的效率比较慢，所以说用到mapstruct

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>1.5.5.Final</version>
</dependency>
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct-processor</artifactId>
    <version>1.5.5.Final</version>
</dependency>
```

首先引入依赖

之后就可以直接添加@mapper注解来实现功能。

**返回类就是我们需要将xx转为什么**

**里面的参数就是我们要转换的类**

```java
@Mapper
public interface UserConverter {
    UserConverter INSTANCE= Mappers.getMapper(UserConverter.class);

    UserDto convertReqToDto(UserReq userReq);

    UserPo convertDtoToUserPo(UserDto userDto);
}
```

之后我们在使用的时候就可以

```java
@PostMapping
public R<Integer> insert(@RequestBody UserReq userReq) {
    UserDto userDto = UserConverter.INSTANCE.convertReqToDto(userReq);
    int i = userService.addUser(userDto);
    return R.ok(i);
}
```

直接进行一个转换了

同时当属性名不一样的时候，可以通过@mapping来进行设置。

## swagger管理

这里不会写swagger管理。

原因是：我平常的习惯是用apifox的，所以这里就不多此一举了。

## RedisTemplate集成

首先进行一个配置

```yml
data:
  redis:
    host: 127.0.0.1
    port: 6379
    database: 5
    lettuce:
      pool:
        max-active: 20
        max-idle: 10
```

之后配置一下我们的自定义序列化

```java
package com.xiaou.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.*;


@SpringBootConfiguration
public class RedisConfig {
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory){
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置value的序列化方式json
        redisTemplate.setValueSerializer(redisSerializer());
        //设置key序列化方式String
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        //设置hash key序列化方式String
        redisTemplate.setHashKeySerializer(new StringRedisSerializer());
        //设置hash value序列化json
        redisTemplate.setHashValueSerializer(redisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    public RedisSerializer<Object> redisSerializer() {
        //创建JSON序列化器
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        //必须设置，否则无法序列化实体类对象
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        return new GenericJackson2JsonRedisSerializer(objectMapper);
    }
}
```

![image-20250414193947235](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504141939473.png)

之后进行一个set测试就可以看到没问题了。

之后我们封装一个RedisUtils类 这个可以根据实际开发去自己封装

这个我是直接用的ruoyi的封装类

这里的RedisUtils类

## 缓存预热

就是项目启动的时候预热一些缓存

缓存预热有很多的方法，这里用到的是一个实现CommandLineRunner

```java
@Component
public class InitCache implements CommandLineRunner {
    @Override
    public void run(String... args) throws Exception {
        System.out.println("初始化缓存");
        //要知道那些缓存需要进行一个预热
        //调用init方法
    }
}
```

这样就可以去实现一些你的逻辑

##  手写redis分布式锁

分布式锁，我们需要的方法分为

- 加锁
- 解锁
- 尝试锁

首先来看加锁的过程

```java
public boolean lock(String lockKey, String requestId, Long expireTime) {
    //1.参数的校验
    if (StringUtils.isBlank(lockKey) || StringUtils.isBlank(requestId) || expireTime < 0) {
        throw new ShareLockException("加锁参数异常");
    }
    long currentTime = System.currentTimeMillis();
    long outTime = currentTime + TIME_OUT;
    Boolean result = false;
    //2.加锁可以自旋
    while (currentTime < outTime) {
        //3.借助redis的setnx命令
        result = redisUtils.setNx(lockKey, requestId, expireTime);
        if (result) {
            return result;
        }
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        currentTime = System.currentTimeMillis();
    }
    return result;
}
```

这里用到的setNx是一个封装就是用到了

```java
public <K, V> Boolean setNx(K key, V value, long expire) {
    return redisTemplate.opsForValue().setIfAbsent(key, value, expire, TimeUnit.MILLISECONDS);
}
```

setIfAbsent的这个方法。

删除锁就是

```java
public boolean unlock(String key, String requestId) {
    if (StringUtils.isBlank(key) || StringUtils.isBlank(requestId)) {
        throw new ShareLockException("解锁参数异常");
    }
    try {
        String value = redisUtils.get(key);
        if (requestId.equals(value)) {
            redisUtils.del(key);
            return true;
        }
    } catch (Exception e) {
        throw new ShareLockException("解锁异常");
    }
    return false;
}
```

之后是尝试加锁

```java
public boolean tryLock(String lockKey, String requestId, Long expireTime) {
    if (StringUtils.isBlank(lockKey) || StringUtils.isBlank(requestId) || expireTime < 0) {
        throw new ShareLockException("加锁参数异常");
    }
    return redisUtils.setNx(lockKey, requestId, expireTime);
}
```

就是我们不管他加没加成功，直接这样写就可以了

分布式的场景说明：

- 任务调度，集群系统
- **操作同一数据**

## spring实现注解缓存方式

这个是一个不常用的扩展。

可以直接看提交记录就可以了。

## Aop实现操作日志记录

就是一个简单的日志打印 相当于一个拦截器。

```java
package com.xiaou.log;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 日志切面
 */
@Aspect
@Slf4j
@Component
@ConditionalOnProperty(name = "xiaou.log.aspect.enabled", havingValue = "true", matchIfMissing = true)
public class LogAspect {

    @Pointcut("execution(* com.xiaou.*.contoller.*Controller.*(..)) || execution(* com.xiaou.*.service.*Service.*(..))")
    private void pointCut() {
    }

    @Around("pointCut()")
    public void around(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        Object[] args = proceedingJoinPoint.getArgs();
        String req = new Gson().toJson(args);
        MethodSignature methodSignature = (MethodSignature) proceedingJoinPoint.getSignature();
        String methodName = methodSignature.getDeclaringType().getName() + "." + methodSignature.getName();
        log.info("请求方法：{}，请求参数：{}", methodName, req);
        Long startTime = System.currentTimeMillis();
        Object respobj = proceedingJoinPoint.proceed();
        String resp = new Gson().toJson(respobj);
        Long endTime = System.currentTimeMillis();
        log.info("，响应参数：{}，耗时：{}ms", resp, endTime - startTime);
    }
}
```

之后就有结果了

![image-20250415153535413](https://xiaou-1305448902.cos.ap-nanjing.myqcloud.com/img/202504151535597.png)

## 结尾

项目闭档了。

因为这个视频后面感觉不太好了，所以就写这么多，这个项目至此结束。