package com.amit.seatreservation.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_show_quotas")
public class UserShowQuota {

    @EmbeddedId
    private UserShowQuotaId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("showId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_id", nullable = false)
    private Show show;

    @Column(name = "active_seats", nullable = false)
    private Integer activeSeats = 0;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public UserShowQuotaId getId() {
        return id;
    }

    public void setId(UserShowQuotaId id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Show getShow() {
        return show;
    }

    public void setShow(Show show) {
        this.show = show;
    }

    public Integer getActiveSeats() {
        return activeSeats;
    }

    public void setActiveSeats(Integer activeSeats) {
        this.activeSeats = activeSeats;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}