package com.tireshop.dao;

import com.tireshop.model.Product;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

/**
 * Data Access Object for Product entities
 */
public class ProductDao extends HibernateDao<Product, Long> {
    
    public ProductDao(SessionFactory sessionFactory) {
        super(Product.class, sessionFactory);
    }
    
    /**
     * Find products by category
     * @param category Product category
     * @return List of products in the category
     */
    public List<Product> findByCategory(String category) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                    "FROM Product WHERE category = :category", Product.class);
            query.setParameter("category", category);
            return query.list();
        }
    }
    
    /**
     * Search products by name or description
     * @param searchTerm Search term
     * @return List of matching products
     */
    public List<Product> search(String searchTerm) {
        try (Session session = sessionFactory.openSession()) {
            String searchPattern = "%" + searchTerm.toLowerCase() + "%";
            Query<Product> query = session.createQuery(
                    "FROM Product WHERE LOWER(name) LIKE :searchTerm OR " +
                    "LOWER(description) LIKE :searchTerm OR " +
                    "LOWER(manufacturer) LIKE :searchTerm OR " +
                    "LOWER(modelNumber) LIKE :searchTerm OR " +
                    "LOWER(size) LIKE :searchTerm OR " +
                    "LOWER(barcode) LIKE :searchTerm OR " +
                    "LOWER(sku) LIKE :searchTerm", 
                    Product.class);
            query.setParameter("searchTerm", searchPattern);
            return query.list();
        }
    }
    
    /**
     * Find products with low inventory
     * @param threshold Low inventory threshold
     * @return List of products below threshold
     */
    public List<Product> findLowInventory(int threshold) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                    "FROM Product WHERE quantityInStock <= :threshold", 
                    Product.class);
            query.setParameter("threshold", threshold);
            return query.list();
        }
    }
    
    /**
     * Find product by barcode
     * @param barcode Product barcode
     * @return Optional containing the product if found
     */
    public Optional<Product> findByBarcode(String barcode) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                    "FROM Product WHERE barcode = :barcode", 
                    Product.class);
            query.setParameter("barcode", barcode);
            return query.uniqueResultOptional();
        }
    }
    
    /**
     * Find product by SKU
     * @param sku Product SKU
     * @return Optional containing the product if found
     */
    public Optional<Product> findBySku(String sku) {
        try (Session session = sessionFactory.openSession()) {
            Query<Product> query = session.createQuery(
                    "FROM Product WHERE sku = :sku", 
                    Product.class);
            query.setParameter("sku", sku);
            return query.uniqueResultOptional();
        }
    }
} 