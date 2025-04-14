package com.xiaou.web.controller;

import com.xiaou.common.R;
import com.xiaou.entity.PageReqDto;
import com.xiaou.entity.PageRespDto;
import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.entity.po.UserPo;
import com.xiaou.web.service.UserService;
import com.xiaou.web.entity.req.UserReq;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

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

    @DeleteMapping("/{id}")
    public R delete(@PathVariable Integer id) {
        int delete = userService.delete(id);
        return R.ok(delete);
    }

    @GetMapping("/list")
    public R<PageRespDto<UserPo>> getPage(@RequestBody PageReqDto dto) {
        PageRespDto<UserPo> userPage = userService.getUserPage(dto);
        return R.ok(userPage);
    }
}
