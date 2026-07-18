package com.tireshop.dao;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Generic Data Access Object interface for CRUD operations
 * @param <T> Entity type
 * @param <ID> ID type
 */
public interface GenericDao<T, ID extends Serializable> {
    
    /**
     * Save a new entity
     * @param entity Entity to save
     * @return Saved entity
     */
    T save(T entity);
    
    /**
     * Update an existing entity
     * @param entity Entity to update
     * @return Updated entity
     */
    T update(T entity);
    
    /**
     * Find entity by ID
     * @param id Entity ID
     * @return Optional containing the entity if found
     */
    Optional<T> findById(ID id);
    
    /**
     * Get all entities
     * @return List of all entities
     */
    List<T> findAll();
    
    /**
     * Delete an entity
     * @param entity Entity to delete
     * @return true if deleted successfully
     */
    boolean delete(T entity);
    
    /**
     * Delete an entity by ID
     * @param id Entity ID
     * @return true if deleted successfully
     */
    boolean deleteById(ID id);
} 