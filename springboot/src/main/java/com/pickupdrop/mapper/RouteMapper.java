package com.pickupdrop.mapper;

import com.pickupdrop.common.BaseMapper;
import com.pickupdrop.dto.RouteDto;
import com.pickupdrop.entity.Route;
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
