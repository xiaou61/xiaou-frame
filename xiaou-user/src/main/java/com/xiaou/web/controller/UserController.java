package com.xiaou.web.controller;

import com.xiaou.common.R;
import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.service.UserService;
import com.xiaou.web.entity.req.UserReq;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    @PostMapping
    public R<Integer> insert(@RequestBody UserReq userReq) {
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(userReq, userDto);
        int i = userService.addUser(userDto);
        return R.ok(i);
    }
}
