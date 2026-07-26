package com.pickupdrop.mapper;

import com.pickupdrop.common.BaseMapper;
import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.entity.Driver;
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
        DriverDto.Response response = super.toDto(entity, DriverDto.Response.class);
        // hasAccount() is not a bean accessor; ModelMapper won't pick it up.
        response.setHasAccount(entity.hasAccount());
        return response;
    }

    public DriverDto.SummaryResponse toSummaryResponse(Driver entity) {
        return super.toDto(entity, DriverDto.SummaryResponse.class);
    }

    public DriverDto.PublicResponse toPublicResponse(Driver entity) {
        return entity == null ? null : super.toDto(entity, DriverDto.PublicResponse.class);
    }
}
