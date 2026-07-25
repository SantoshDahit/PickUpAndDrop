package com.pickupdrop.repository.driver;

import static com.pickupdrop.entity.QDriver.driver;

import com.pickupdrop.dto.DriverDto;
import com.pickupdrop.entity.Driver;
import com.pickupdrop.enums.DriverStatus;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Wildcard;
import com.querydsl.jpa.impl.JPAQueryFactory;
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
public class DriverQueryRepository {

    private final JPAQueryFactory queryFactory;

    public Page<Driver> search(DriverDto.SearchRequest searchRequest, Pageable pageable) {
        List<Driver> content = queryFactory.selectFrom(driver)
                .where(
                        likeNameIfExists(searchRequest.name()),
                        inStatusListIfExists(searchRequest.statusList()),
                        goeSeatsIfExists(searchRequest.minSeats()),
                        driver.deletedAt.isNull()
                )
                .orderBy(driver.name.asc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        Long totalCount = Optional.ofNullable(
                queryFactory.select(Wildcard.count)
                        .from(driver)
                        .where(
                                likeNameIfExists(searchRequest.name()),
                                inStatusListIfExists(searchRequest.statusList()),
                                goeSeatsIfExists(searchRequest.minSeats()),
                                driver.deletedAt.isNull()
                        )
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, totalCount);
    }

    private BooleanExpression likeNameIfExists(String name) {
        if (Objects.isNull(name) || name.isBlank()) {
            return null;
        }
        return driver.name.contains(name);
    }

    private BooleanExpression inStatusListIfExists(List<DriverStatus> statusList) {
        if (Objects.isNull(statusList) || statusList.isEmpty()) {
            return null;
        }
        return driver.status.in(statusList);
    }

    private BooleanExpression goeSeatsIfExists(Integer minSeats) {
        if (Objects.isNull(minSeats)) {
            return null;
        }
        return driver.seats.goe(minSeats);
    }
}
