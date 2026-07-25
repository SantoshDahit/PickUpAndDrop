package com.landgreet.mapper;

import com.landgreet.common.BaseMapper;
import com.landgreet.dto.RouteDto;
import com.landgreet.entity.Route;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class RouteMapper extends BaseMapper<Route, RouteDto> {

    protected RouteMapper(ModelMapper modelMapper) {
        super(modelMapper, Route.class);
        this.registerDtoMapping(RouteDto.Response.class);
    }

    public RouteDto.Response toResponse(Route entity) {
        return super.toDto(entity, RouteDto.Response.class);
    }
}
