package com.vanmo.booking_app_be.repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.TourDestinationDTO;
import com.vanmo.booking_app_be.entity.Tour;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

public interface TourRepository extends JpaRepository<Tour, Long> {
    List<Tour> findAllByHotel_Id(Long hotelId);

    /**
     * Running tours of open branches, for guests browsing outside a hotel page. The text matches the tour's name, its
     * destination or the branch's address (MySQL's default collation ignores case and Vietnamese accents).
     */
    @Query("select t from Tour t join t.hotel h where t.available = true and h.active = true "
            + "and (:q is null or t.name like concat('%', :q, '%') or t.destination like concat('%', :q, '%') "
            + "or h.address like concat('%', :q, '%')) "
            + "and (:theme is null or :theme member of t.themes) "
            + "and (:maxPrice is null or t.price <= :maxPrice)")
    Page<Tour> searchPublic(
            @Param("q") String q,
            @Param("theme") TravelTheme theme,
            @Param("maxPrice") BigDecimal maxPrice,
            Pageable pageable);

    /** Every running tour of an open branch - the home screen picks from these. */
    @Query("select t from Tour t join t.hotel h where t.available = true and h.active = true")
    List<Tour> findAllPublic();

    /** Destinations guests can pick from, most tours first. */
    @Query("select new com.vanmo.booking_app_be.dto.response.TourDestinationDTO("
            + "t.destination, count(t), min(t.price), max(t.pathImage)) "
            + "from Tour t join t.hotel h where t.available = true and h.active = true and t.destination is not null "
            + "group by t.destination order by count(t) desc, t.destination")
    List<TourDestinationDTO> findDestinations();
}
