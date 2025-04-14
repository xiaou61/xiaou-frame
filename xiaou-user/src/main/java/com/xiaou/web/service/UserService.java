package com.xiaou.web.service;

import com.xiaou.entity.PageReqDto;
import com.xiaou.entity.PageRespDto;
import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.entity.po.UserPo;

public interface UserService {
    int addUser(UserDto userDto);

    int delete(Integer id);

    PageRespDto<UserPo> getUserPage(PageReqDto pageReqDto);
}
