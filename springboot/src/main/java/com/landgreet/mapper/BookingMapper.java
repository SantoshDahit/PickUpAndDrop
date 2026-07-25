package com.landgreet.mapper;

import com.landgreet.common.BaseMapper;
import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Booking;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
public class BookingMapper extends BaseMapper<Booking, BookingDto> {

    private final DriverMapper driverMapper;

    protected BookingMapper(ModelMapper modelMapper, DriverMapper driverMapper) {
        super(modelMapper, Booking.class);
        this.driverMapper = driverMapper;
        this.registerDtoMapping(BookingDto.Response.class);
        this.registerDtoMapping(BookingDto.SummaryResponse.class);
    }

    public BookingDto.Response toResponse(Booking entity) {
        BookingDto.Response response = super.toDto(entity, BookingDto.Response.class);
        // travelGroup.id → groupId is not a STRICT-matching path; set explicitly.
        response.setGroupId(entity.getTravelGroup() == null ? null : entity.getTravelGroup().getId());
        response.setDriver(driverMapper.toPublicResponse(entity.effectiveDriver()));
        return response;
    }

    public BookingDto.SummaryResponse toSummaryResponse(Booking entity) {
        BookingDto.SummaryResponse response = super.toDto(entity, BookingDto.SummaryResponse.class);
        response.setGroupId(entity.getTravelGroup() == null ? null : entity.getTravelGroup().getId());
        response.setDriver(driverMapper.toPublicResponse(entity.effectiveDriver()));
        return response;
    }
}
