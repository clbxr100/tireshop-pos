package com.tireshop.dao;

import com.tireshop.model.User;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import java.util.List;
import java.util.Optional;

public class UserDao extends HibernateDao<User, String> {

    public UserDao(SessionFactory sessionFactory) {
        super(User.class, sessionFactory);
    }

    public Optional<User> findByUsername(String username) {
        try (Session session = this.sessionFactory.openSession()) {
            Query<User> query = session.createQuery("FROM User WHERE username = :username", User.class);
            query.setParameter("username", username);
            return Optional.ofNullable(query.uniqueResult());
        }
    }

    // Optional: If you want to check if a username already exists quickly
    public boolean usernameExists(String username) {
        return findByUsername(username).isPresent();
    }

    // Save or update a user
    public User saveOrUpdateUser(User user) {
        try (Session session = this.sessionFactory.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.saveOrUpdate(user);
                tx.commit();
                System.out.println("[UserDao] Saved/Updated user: " + user.getUsername());
                return user;
            } catch (Exception e) {
                if (tx != null) tx.rollback();
                // Consider logging the exception e.g., using a logger
                System.err.println("UserDao: Error during saveOrUpdateUser for user " + user.getUsername() + ": " + e.getMessage());
                e.printStackTrace();
                throw e; // Or handle more gracefully
            }
        }
    }
    
    // Override delete to ensure it works as expected, or rely on superclass implementation
    // public void delete(User user) { super.delete(user); }
    // public void deleteById(String id) { super.deleteById(id); }
} 