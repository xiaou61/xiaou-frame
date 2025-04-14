package com.xiaou.web.entity.po;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.xiaou.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@TableName("user")
@Data
public class UserPo extends BaseEntity {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String name;

    private Integer age;


}
