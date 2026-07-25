package com.pickupdrop.mapper;

import com.pickupdrop.common.BaseMapper;
import com.pickupdrop.dto.UserDto;
import com.pickupdrop.entity.User;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class UserMapper extends BaseMapper<User, UserDto> {

    protected UserMapper(ModelMapper modelMapper) {
        super(modelMapper, User.class);
        this.registerDtoMapping(UserDto.Response.class);
    }

    public UserDto.Response toResponse(User entity) {
        return super.toDto(entity, UserDto.Response.class);
    }
}
