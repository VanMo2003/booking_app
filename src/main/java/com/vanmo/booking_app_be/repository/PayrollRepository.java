package com.vanmo.booking_app_be.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.dto.response.PayrollCostRow;
import com.vanmo.booking_app_be.entity.Payroll;
import com.vanmo.booking_app_be.entity.enums.PayrollStatus;

public interface PayrollRepository extends JpaRepository<Payroll, Long> {

    /** Payslips in the statuses for the branches' staff whose month date falls in [from, to]. */
    @Query("select new com.vanmo.booking_app_be.dto.response.PayrollCostRow(p.month, p.totalSalary) "
            + "from Payroll p join p.employee e "
            + "where e.hotel.id in :hotelIds and p.status in :statuses and p.month between :from and :to")
    List<PayrollCostRow> findCosts(
            @Param("hotelIds") List<Long> hotelIds,
            @Param("statuses") Collection<PayrollStatus> statuses,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);
}
