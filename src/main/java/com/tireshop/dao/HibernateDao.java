package com.tireshop.dao;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Generic implementation of GenericDao using Hibernate
 * @param <T> Entity type
 * @param <ID> ID type
 */
public class HibernateDao<T, ID extends Serializable> implements GenericDao<T, ID> {
    
    protected final Class<T> entityClass;
    protected final SessionFactory sessionFactory;
    
    public HibernateDao(Class<T> entityClass, SessionFactory sessionFactory) {
        this.entityClass = entityClass;
        this.sessionFactory = sessionFactory;
    }
    
    @Override
    public T save(T entity) {
        // Wrap with retry logic for connection failures
        return com.tireshop.util.ConnectionResilience.executeWithRetry(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                session.save(entity);
                session.flush();  // Force immediate write to database to prevent duplicate invoice numbers
                transaction.commit();
                return entity;
            } catch (Exception e) {
                if (transaction != null) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackEx) {
                        // Ignore rollback errors
                    }
                }
                throw e;
            }
        }, 3); // Retry up to 3 times
    }
    
    @Override
    public T update(T entity) {
        // Wrap with retry logic for connection failures
        return com.tireshop.util.ConnectionResilience.executeWithRetry(() -> {
            Transaction transaction = null;
            try (Session session = sessionFactory.openSession()) {
                transaction = session.beginTransaction();
                T mergedEntity = (T) session.merge(entity);  // Use merge instead of update for detached entities
                session.flush();  // Force immediate write to database
                transaction.commit();
                session.clear();  // Clear session cache to prevent stale data
                return mergedEntity;
            } catch (Exception e) {
                if (transaction != null) {
                    try {
                        transaction.rollback();
                    } catch (Exception rollbackEx) {
                        // Ignore rollback errors
                    }
                }
                throw e;
            }
        }, 3); // Retry up to 3 times
    }
    
    @Override
    public Optional<T> findById(ID id) {
        // Wrap with retry logic for connection failures
        return com.tireshop.util.ConnectionResilience.executeWithRetry(() -> {
            try (Session session = sessionFactory.openSession()) {
                T entity = session.get(entityClass, id);
                return Optional.ofNullable(entity);
            }
        }, 3); // Retry up to 3 times
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public List<T> findAll() {
        // Wrap with retry logic for connection failures
        return com.tireshop.util.ConnectionResilience.executeWithRetry(() -> {
            try (Session session = sessionFactory.openSession()) {
                return session.createQuery("FROM " + entityClass.getSimpleName()).list();
            }
        }, 3); // Retry up to 3 times
    }
    
    @Override
    public boolean delete(T entity) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            session.delete(entity);
            transaction.commit();
            return true;
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting entity: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean deleteById(ID id) {
        Transaction transaction = null;
        try (Session session = sessionFactory.openSession()) {
            transaction = session.beginTransaction();
            T entity = session.get(entityClass, id);
            if (entity != null) {
                session.delete(entity);
                transaction.commit();
                return true;
            } else {
                transaction.rollback();
                return false;
            }
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            throw new RuntimeException("Error deleting entity by ID: " + e.getMessage(), e);
        }
    }
} 