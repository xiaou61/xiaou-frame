package com.xiaou.web.convert;


import com.xiaou.web.entity.dto.UserDto;
import com.xiaou.web.entity.po.UserPo;
import com.xiaou.web.entity.req.UserReq;
import org.mapstruct.Mapper;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserConverter {
    UserConverter INSTANCE= Mappers.getMapper(UserConverter.class);

    UserDto convertReqToDto(UserReq userReq);

    UserPo convertDtoToUserPo(UserDto userDto);
}
