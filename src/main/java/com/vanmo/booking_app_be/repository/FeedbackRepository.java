package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.FeedbackRow;
import com.vanmo.booking_app_be.entity.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    /** Reviews of the branches' bookings written in [from, to], newest first. */
    @Query("select new com.vanmo.booking_app_be.dto.response.FeedbackRow("
            + "f.id, b.id, h.id, h.name, c.fullName, f.rating, f.comments, f.reviewDate) "
            + "from Feedback f join f.booking b join b.hotel h join f.customer c "
            + "where h.id in :hotelIds and f.reviewDate between :from and :to "
            + "order by f.reviewDate desc, f.id desc")
    List<FeedbackRow> findForReport(
            @Param("hotelIds") Collection<Long> hotelIds, @Param("from") LocalDate from, @Param("to") LocalDate to);
}
