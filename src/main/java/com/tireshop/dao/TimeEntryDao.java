package com.tireshop.dao;

import com.tireshop.model.TimeEntry;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TimeEntryDao extends HibernateDao<TimeEntry, String> {

    public TimeEntryDao(SessionFactory sessionFactory) {
        super(TimeEntry.class, sessionFactory);
    }

    // Renamed: For saving new time entries
    public TimeEntry saveTimeEntry(TimeEntry timeEntry) {
        return super.save(timeEntry); // Use inherited save
    }

    // New method: For updating existing time entries
    public TimeEntry updateTimeEntry(TimeEntry timeEntry) {
        return super.update(timeEntry); // Use inherited update
    }

    // Method to find time entries by user ID
    public List<TimeEntry> findByUserId(String userId) {
        try (Session session = sessionFactory.openSession()) {
            Query<TimeEntry> query = session.createQuery("FROM TimeEntry WHERE userId = :userId ORDER BY clockIn DESC", TimeEntry.class);
            query.setParameter("userId", userId);
            return query.list();
        }
    }

    // Method to find time entries within a date range
    public List<TimeEntry> findByDateRange(LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTimePlusOneDay = endDate.plusDays(1).atStartOfDay();

            Query<TimeEntry> query = session.createQuery(
                "FROM TimeEntry WHERE clockIn >= :startDateTime AND clockIn < :endDateTimePlusOneDay ORDER BY clockIn DESC", 
                TimeEntry.class);
            query.setParameter("startDateTime", startDateTime);
            query.setParameter("endDateTimePlusOneDay", endDateTimePlusOneDay);
            return query.list();
        }
    }

    // Method to find time entries by user ID and date range
    public List<TimeEntry> findByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        try (Session session = sessionFactory.openSession()) {
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTimePlusOneDay = endDate.plusDays(1).atStartOfDay();

            Query<TimeEntry> query = session.createQuery(
                "FROM TimeEntry WHERE userId = :userId AND clockIn >= :startDateTime AND clockIn < :endDateTimePlusOneDay ORDER BY clockIn DESC", 
                TimeEntry.class);
            query.setParameter("userId", userId);
            query.setParameter("startDateTime", startDateTime);
            query.setParameter("endDateTimePlusOneDay", endDateTimePlusOneDay);
            return query.list();
        }
    }
} 