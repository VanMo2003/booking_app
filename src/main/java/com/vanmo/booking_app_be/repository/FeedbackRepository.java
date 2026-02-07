package com.vanmo.booking_app_be.repository;

import com.vanmo.booking_app_be.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
}
