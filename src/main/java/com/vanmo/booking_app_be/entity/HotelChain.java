package com.vanmo.booking_app_be.entity;

import java.util.Date;
import java.util.Set;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.vanmo.booking_app_be.entity.enums.ApprovalStatus;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@Table(name = "hotel_chains")
public class HotelChain {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotNull
    String name;

    @Column(length = 5000, columnDefinition = "TEXT")
    String description;

    String pathImage;

    // Contact details given at owner registration; the review result is emailed to `email`.
    String ownerName;
    String email;
    String phone;
    String address;

    /**
     * An admin reviews every owner registration before the owner may run the hotel. Rows created before the review
     * flow existed are migrated to APPROVED on startup (see ApplicationInitConfig); {@link #isApproved()} still treats
     * a null as approved.
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    ApprovalStatus approvalStatus;

    /** Why the last submission was rejected - kept after a resubmission so the reviewer has the history. */
    @Column(length = 1000)
    String rejectionReason;

    Date submittedAt;
    Date reviewedAt;

    @OneToOne
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    Account account;

    @OneToMany(mappedBy = "hotelChain")
    Set<Hotel> hotels;

    @CreationTimestamp
    @Column(updatable = false)
    Date onCreate;

    @UpdateTimestamp
    Date onUpdate;

    public boolean isApproved() {
        return approvalStatus == null || approvalStatus == ApprovalStatus.APPROVED;
    }
}
