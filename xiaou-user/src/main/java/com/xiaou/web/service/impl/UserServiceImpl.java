package com.xiaou.web.service.impl;

import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.service.UserService;
import com.xiaou.web.entity.po.UserPo;
import com.xiaou.web.mapper.UserMapper;
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
