package com.landgreet.mapper;

import com.landgreet.common.BaseMapper;
import com.landgreet.dto.DriverDto;
import com.landgreet.entity.Driver;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class DriverMapper extends BaseMapper<Driver, DriverDto> {

    protected DriverMapper(ModelMapper modelMapper) {
        super(modelMapper, Driver.class);
        this.registerDtoMapping(DriverDto.Response.class);
        this.registerDtoMapping(DriverDto.SummaryResponse.class);
        this.registerDtoMapping(DriverDto.PublicResponse.class);
    }

    public DriverDto.Response toResponse(Driver entity) {
        return super.toDto(entity, DriverDto.Response.class);
    }

    public DriverDto.SummaryResponse toSummaryResponse(Driver entity) {
        return super.toDto(entity, DriverDto.SummaryResponse.class);
    }

    public DriverDto.PublicResponse toPublicResponse(Driver entity) {
        return entity == null ? null : super.toDto(entity, DriverDto.PublicResponse.class);
    }
}
