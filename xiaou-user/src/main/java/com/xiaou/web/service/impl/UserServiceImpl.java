package com.xiaou.web.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xiaou.entity.PageReqDto;
import com.xiaou.entity.PageRespDto;
import com.xiaou.web.convert.UserConverter;
import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.service.UserService;
import com.xiaou.web.entity.po.UserPo;
import com.xiaou.web.mapper.UserMapper;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;


@Service
public class UserServiceImpl implements UserService {


    @Resource
    private UserMapper userMapper;

    @Override
    public Integer addUser(UserDto userDto) {
        UserPo userPo = UserConverter.INSTANCE.convertDtoToUserPo(userDto);
        int count = userMapper.insert(userPo);
        return count;
    }

    @Override
    public Integer delete(Integer id) {
        int i = userMapper.deleteById(id);
        return i;
    }

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

    @Override
    @Cacheable(cacheNames = "user", key = "'getUserById' + #id")
    public UserPo getUserById(Integer id) {
        return userMapper.selectById(id);
    }
}
