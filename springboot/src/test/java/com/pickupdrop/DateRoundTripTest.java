package com.pickupdrop;

import static org.assertj.core.api.Assertions.assertThat;

import com.pickupdrop.dto.BookingDto;
import com.pickupdrop.enums.MatchPref;
import com.pickupdrop.service.BookingFacade;
import com.pickupdrop.support.IntegrationTestBase;
import com.pickupdrop.support.TestDataHelper;
import jakarta.persistence.EntityManager;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Landing dates drive the week buckets, so a one-day drift is a correctness
 * bug, not a cosmetic one. The DB session and the JVM must agree on the zone:
 * with Hibernate's jdbc.time_zone set to Asia/Seoul against a UTC MySQL
 * session, midnight KST lands in the previous UTC day and DATE loses a day.
 */
@Import(TestDataHelper.class)
class DateRoundTripTest extends IntegrationTestBase {

    @Autowired
    private TestDataHelper dataHelper;

    @Autowired
    private BookingFacade bookingFacade;

    @Autowired
    private EntityManager entityManager;

    @Test
    void travelDateSurvivesAWriteAndReRead() {
        var user = dataHelper.createUser();
        LocalDate requested = LocalDate.of(2026, 12, 15);

        BookingDto.Response created = bookingFacade.create(user.getId(), new BookingDto.PostRequest(
                dataHelper.firstRoute().getId(), null, requested, null, 1, MatchPref.GROUP,
                null, null, null));
        assertThat(created.getTravelDate()).isEqualTo(requested);

        // force a real round trip through MySQL rather than the first-level cache
        entityManager.flush();
        entityManager.clear();

        var reread = bookingFacade.getMyBookings(user.getId()).get(0);
        assertThat(reread.getTravelDate())
                .as("date must not drift when written and read back")
                .isEqualTo(requested);
    }
}
