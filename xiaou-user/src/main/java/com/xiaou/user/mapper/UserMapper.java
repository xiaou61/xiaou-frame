package com.xiaou.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xiaou.user.entity.po.UserPo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserPo> {
}
