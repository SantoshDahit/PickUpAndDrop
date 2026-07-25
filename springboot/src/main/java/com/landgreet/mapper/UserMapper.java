package com.landgreet.mapper;

import com.landgreet.common.BaseMapper;
import com.landgreet.dto.UserDto;
import com.landgreet.entity.User;
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
