package com.tireshop.dao;

import com.tireshop.model.TireData;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.query.Query;

import java.util.List;
import java.util.Optional;

public class TireDataDao extends HibernateDao<TireData, Long> {

    public TireDataDao(SessionFactory sessionFactory) {
        super(TireData.class, sessionFactory);
    }

    /**
     * Find tire data by barcode
     */
    public Optional<TireData> findByBarcode(String barcode) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE barcode = :barcode", TireData.class);
            query.setParameter("barcode", barcode);
            return query.uniqueResultOptional();
        }
    }

    /**
     * Find tire data by GTIN
     */
    public Optional<TireData> findByGtin(String gtin) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE gtin = :gtin", TireData.class);
            query.setParameter("gtin", gtin);
            return query.uniqueResultOptional();
        }
    }

    /**
     * Find tire data by barcode or GTIN
     */
    public Optional<TireData> findByBarcodeOrGtin(String code) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE barcode = :code OR gtin = :code", TireData.class);
            query.setParameter("code", code);
            return query.uniqueResultOptional();
        }
    }

    /**
     * Find tire data by SKU
     */
    public Optional<TireData> findBySku(String sku) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE sku = :sku", TireData.class);
            query.setParameter("sku", sku);
            return query.uniqueResultOptional();
        }
    }

    /**
     * Find all tires by brand
     */
    public List<TireData> findByBrand(String brand) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE brand = :brand ORDER BY name", TireData.class);
            query.setParameter("brand", brand);
            return query.list();
        }
    }

    /**
     * Find all tires by size
     */
    public List<TireData> findBySize(String size) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE size = :size ORDER BY brand, name", TireData.class);
            query.setParameter("size", size);
            return query.list();
        }
    }

    /**
     * Find tires by brand and size
     */
    public List<TireData> findByBrandAndSize(String brand, String size) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE brand = :brand AND size = :size ORDER BY name", TireData.class);
            query.setParameter("brand", brand);
            query.setParameter("size", size);
            return query.list();
        }
    }

    /**
     * Search tires by text (brand, name, or size)
     */
    public List<TireData> searchTires(String searchText) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE " +
                "LOWER(brand) LIKE LOWER(:searchText) OR " +
                "LOWER(name) LIKE LOWER(:searchText) OR " +
                "LOWER(size) LIKE LOWER(:searchText) OR " +
                "LOWER(sku) LIKE LOWER(:searchText) " +
                "ORDER BY brand, name", TireData.class);
            query.setParameter("searchText", "%" + searchText + "%");
            return query.list();
        }
    }

    /**
     * Find tires with low stock
     */
    public List<TireData> findLowStockTires() {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE stockQty <= reorderPoint ORDER BY stockQty ASC", TireData.class);
            return query.list();
        }
    }

    /**
     * Find tires by season
     */
    public List<TireData> findBySeason(String season) {
        try (Session session = sessionFactory.openSession()) {
            Query<TireData> query = session.createQuery(
                "FROM TireData WHERE season = :season ORDER BY brand, name", TireData.class);
            query.setParameter("season", season);
            return query.list();
        }
    }

    /**
     * Get all unique brands
     */
    public List<String> getAllBrands() {
        try (Session session = sessionFactory.openSession()) {
            Query<String> query = session.createQuery(
                "SELECT DISTINCT brand FROM TireData ORDER BY brand", String.class);
            return query.list();
        }
    }

    /**
     * Get all unique sizes
     */
    public List<String> getAllSizes() {
        try (Session session = sessionFactory.openSession()) {
            Query<String> query = session.createQuery(
                "SELECT DISTINCT size FROM TireData ORDER BY size", String.class);
            return query.list();
        }
    }

    /**
     * Get all unique seasons
     */
    public List<String> getAllSeasons() {
        try (Session session = sessionFactory.openSession()) {
            Query<String> query = session.createQuery(
                "SELECT DISTINCT season FROM TireData ORDER BY season", String.class);
            return query.list();
        }
    }

    /**
     * Save or update tire data (with duplicate handling)
     */
    public TireData saveOrUpdateTireData(TireData tireData) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            // Check for existing tire by SKU, barcode, or GTIN
            TireData existing = null;
            if (tireData.getSku() != null) {
                Query<TireData> skuQuery = session.createQuery(
                    "FROM TireData WHERE sku = :sku", TireData.class);
                skuQuery.setParameter("sku", tireData.getSku());
                existing = skuQuery.uniqueResult();
            }
            
            if (existing == null && tireData.getBarcode() != null) {
                Query<TireData> barcodeQuery = session.createQuery(
                    "FROM TireData WHERE barcode = :barcode", TireData.class);
                barcodeQuery.setParameter("barcode", tireData.getBarcode());
                existing = barcodeQuery.uniqueResult();
            }
            
            if (existing == null && tireData.getGtin() != null) {
                Query<TireData> gtinQuery = session.createQuery(
                    "FROM TireData WHERE gtin = :gtin", TireData.class);
                gtinQuery.setParameter("gtin", tireData.getGtin());
                existing = gtinQuery.uniqueResult();
            }
            
            if (existing != null) {
                // Update existing record with new information
                updateExistingTireData(existing, tireData);
                session.merge(existing);
                session.getTransaction().commit();
                return existing;
            } else {
                // Save new tire data
                session.save(tireData);
                session.getTransaction().commit();
                return tireData;
            }
        }
    }

    /**
     * Update existing tire data with new information from scraped data
     */
    private void updateExistingTireData(TireData existing, TireData newData) {
        // Update fields that might have changed
        if (newData.getPrice() != null && !newData.getPrice().isEmpty()) {
            existing.setPrice(newData.getPrice());
        }
        if (newData.getCostPrice() != null && !newData.getCostPrice().isEmpty()) {
            existing.setCostPrice(newData.getCostPrice());
        }
        if (newData.getStockQty() != null) {
            existing.setStockQty(newData.getStockQty());
        }
        if (newData.getAvailableQty() != null) {
            existing.setAvailableQty(newData.getAvailableQty());
        }
        if (newData.getRating() != null && !newData.getRating().isEmpty()) {
            existing.setRating(newData.getRating());
        }
        if (newData.getWarehouse() != null && !newData.getWarehouse().isEmpty()) {
            existing.setWarehouse(newData.getWarehouse());
        }
        if (newData.getSupplierCode() != null && !newData.getSupplierCode().isEmpty()) {
            existing.setSupplierCode(newData.getSupplierCode());
        }
        if (newData.getLastReceived() != null && !newData.getLastReceived().isEmpty()) {
            existing.setLastReceived(newData.getLastReceived());
        }
        if (newData.getGtin() != null && !newData.getGtin().isEmpty() && existing.getGtin() == null) {
            existing.setGtin(newData.getGtin());
        }
        
        // Update source to indicate multiple sources if different
        if (newData.getSource() != null && !newData.getSource().equals(existing.getSource())) {
            String currentSource = existing.getSource();
            if (currentSource == null || currentSource.isEmpty()) {
                existing.setSource(newData.getSource());
            } else if (!currentSource.contains(newData.getSource())) {
                existing.setSource(currentSource + "," + newData.getSource());
            }
        }
    }

    /**
     * Bulk save tire data list
     */
    public void saveTireDataBatch(List<TireData> tireDataList) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            
            for (int i = 0; i < tireDataList.size(); i++) {
                TireData tireData = tireDataList.get(i);
                saveOrUpdateTireData(tireData);
                
                // Flush and clear session every 50 records to avoid memory issues
                if (i % 50 == 0) {
                    session.flush();
                    session.clear();
                }
            }
            
            session.getTransaction().commit();
        }
    }
} 