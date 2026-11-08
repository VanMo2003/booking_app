package com.vanmo.booking_app_be.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.vanmo.booking_app_be.entity.Account;
import com.vanmo.booking_app_be.entity.Customer;
import com.vanmo.booking_app_be.entity.enums.TravelTheme;

public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Customer findByAccount_Id(String accountId);

    /** Accounts of guests interested in any of the themes. */
    @Query("select distinct a from Customer c join c.account a join c.interests i where i in :themes")
    List<Account> findAccountsInterestedIn(@Param("themes") Collection<TravelTheme> themes);

    List<Customer> findAllByPhoneNumber(String phoneNumber);

    /**
     * Walk-in profiles only - used when a returning guest self-registers and claims their profile.
     * A list rather than Optional because nothing stops two walk-ins sharing a phone number.
     */
    List<Customer> findAllByPhoneNumberAndAccountIsNull(String phoneNumber);
}
