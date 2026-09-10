package com.vanmo.booking_app_be.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vanmo.booking_app_be.entity.InvalidatedToken;

@Repository
public interface InvalidatedRepository extends JpaRepository<InvalidatedToken, String> {}
