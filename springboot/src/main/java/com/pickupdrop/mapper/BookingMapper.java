package com.pickupdrop.mapper;

import com.pickupdrop.common.BaseMapper;
import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.entity.Booking;
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
        this.registerDtoMapping(BookingDto.AdminDetailResponse.class);
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

    public BookingDto.AdminDetailResponse toAdminDetailResponse(Booking entity) {
        BookingDto.AdminDetailResponse response = super.toDto(entity, BookingDto.AdminDetailResponse.class);
        response.setGroupId(entity.getTravelGroup() == null ? null : entity.getTravelGroup().getId());
        response.setDriver(driverMapper.toPublicResponse(entity.effectiveDriver()));
        response.setCustomer(new BookingDto.CustomerResponse(
                entity.getUser().getName(), entity.getUser().getEmail(), entity.getUser().getPhone()));
        return response;
    }
}
