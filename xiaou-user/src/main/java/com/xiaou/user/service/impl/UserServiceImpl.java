package com.xiaou.user.service.impl;

import com.xiaou.user.entity.dto.UserDto;
import com.xiaou.user.entity.po.UserPo;
import com.xiaou.user.mapper.UserMapper;
import com.xiaou.user.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {


    @Resource
    private UserMapper userMapper;
    @Override
    public int addUser(UserDto userDto) {
        UserPo userPo = new UserPo();
        BeanUtils.copyProperties(userDto,userPo);
        int count = userMapper.insert(userPo);
        return count;
    }
}
