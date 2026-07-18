package com.tireshop.model;

import java.time.LocalDateTime;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.FetchType;

@Entity
@Table(name = "time_entries")
public class TimeEntry {

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    private String id;

    // Link to the User entity. We store userId as a String corresponding to User.id
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId; 

    // Optional: If you want a direct object relation (requires User to be an entity)
    // @ManyToOne(fetch = FetchType.LAZY) // LAZY to avoid loading User unless needed
    // @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    // private User user; // This would be the actual User object

    @Column(name = "clock_in", nullable = false)
    private LocalDateTime clockIn;

    @Column(name = "clock_out") // Nullable because it's not set until clock-out
    private LocalDateTime clockOut;

    @Column(name = "notes", columnDefinition = "TEXT") // Use TEXT for potentially longer notes
    private String notes;

    public TimeEntry() {
    }

    public TimeEntry(String id, String userId, LocalDateTime clockIn, LocalDateTime clockOut, String notes) {
        this.id = id;
        this.userId = userId;
        this.clockIn = clockIn;
        this.clockOut = clockOut;
        this.notes = notes;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // If using the @ManyToOne User user field:
    // public User getUser() { return user; }
    // public void setUser(User user) { this.user = user; }

    public LocalDateTime getClockIn() {
        return clockIn;
    }

    public void setClockIn(LocalDateTime clockIn) {
        this.clockIn = clockIn;
    }

    public LocalDateTime getClockOut() {
        return clockOut;
    }

    public void setClockOut(LocalDateTime clockOut) {
        this.clockOut = clockOut;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    // Helper method to calculate duration in hours (No JPA annotation needed, it's transient)
    public double getDurationHours() {
        if (clockIn == null || clockOut == null) {
            return 0.0;
        }
        long minutes = java.time.Duration.between(clockIn, clockOut).toMinutes();
        return minutes / 60.0;
    }

    @Override
    public String toString() {
        return "TimeEntry{" +
               "id='" + id + '\'' +
               ", userId='" + userId + '\'' +
               ", clockIn=" + clockIn +
               ", clockOut=" + clockOut +
               ", notes='" + notes + '\'' +
               '}';
    }
} 