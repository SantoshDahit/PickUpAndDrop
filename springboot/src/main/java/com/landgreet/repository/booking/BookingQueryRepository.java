package com.landgreet.repository.booking;

import static com.landgreet.entity.QBooking.booking;

import com.landgreet.dto.BookingDto;
import com.landgreet.entity.Booking;
import com.landgreet.enums.BookingStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class BookingQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Booking> search(BookingDto.SearchRequest searchRequest, Pageable pageable) {
        List<Booking> content = queryFactory.selectFrom(booking)
                .where(
                        eqRouteIdIfExists(searchRequest.routeId()),
                        inStatusListIfExists(searchRequest.statusList()),
                        travelDateBetween(searchRequest.minTravelDate(), searchRequest.maxTravelDate()),
                        booking.deletedAt.isNull()
                )
                .orderBy(booking.travelDate.asc(), booking.createdAt.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = Optional.ofNullable(
                queryFactory.select(Wildcard.count)
                        .from(booking)
                        .where(
                                eqRouteIdIfExists(searchRequest.routeId()),
                                inStatusListIfExists(searchRequest.statusList()),
                                travelDateBetween(searchRequest.minTravelDate(), searchRequest.maxTravelDate()),
                                booking.deletedAt.isNull()
                        )
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression eqRouteIdIfExists(String routeId) {
        if (Objects.isNull(routeId) || routeId.isBlank()) {
            return null;
        }
        return booking.route.id.eq(routeId);
    }

    private BooleanExpression inStatusListIfExists(List<BookingStatus> statusList) {
        if (Objects.isNull(statusList) || statusList.isEmpty()) {
            return null;
        }
        return booking.status.in(statusList);
    }

    private BooleanExpression travelDateBetween(LocalDate minTravelDate, LocalDate maxTravelDate) {
        if (minTravelDate != null && maxTravelDate != null) {
            return booking.travelDate.between(minTravelDate, maxTravelDate);
        } else if (minTravelDate != null) {
            return booking.travelDate.goe(minTravelDate);
        } else if (maxTravelDate != null) {
            return booking.travelDate.loe(maxTravelDate);
        }
        return null;
    }
}
