package com.tireshop.service;

import com.tireshop.dao.ServiceRecordDao;
import com.tireshop.model.*;
import com.tireshop.util.SettingsService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.Optional;

/**
 * Service for managing vehicle service history
 */
public class VehicleServiceHistoryService {
    
    private final ServiceRecordDao serviceRecordDao;
    private final EmailService emailService;
    private final SettingsService settingsService;
    
    // Service type constants
    public static final String SERVICE_TYPE_OIL_CHANGE = "Oil Change";
    public static final String SERVICE_TYPE_TIRE_ROTATION = "Tire Rotation";
    public static final String SERVICE_TYPE_TIRE_REPLACEMENT = "Tire Replacement";
    public static final String SERVICE_TYPE_BRAKE_SERVICE = "Brake Service";
    public static final String SERVICE_TYPE_ALIGNMENT = "Wheel Alignment";
    public static final String SERVICE_TYPE_BATTERY = "Battery Service";
    public static final String SERVICE_TYPE_INSPECTION = "Vehicle Inspection";
    public static final String SERVICE_TYPE_OTHER = "Other Service";
    
    // Service interval recommendations (in days)
    private static final Map<String, Integer> SERVICE_INTERVALS = new HashMap<>();
    static {
        SERVICE_INTERVALS.put(SERVICE_TYPE_OIL_CHANGE, 90); // 3 months
        SERVICE_INTERVALS.put(SERVICE_TYPE_TIRE_ROTATION, 180); // 6 months
        SERVICE_INTERVALS.put(SERVICE_TYPE_BRAKE_SERVICE, 365); // 1 year
        SERVICE_INTERVALS.put(SERVICE_TYPE_ALIGNMENT, 365); // 1 year
        SERVICE_INTERVALS.put(SERVICE_TYPE_BATTERY, 1095); // 3 years
        SERVICE_INTERVALS.put(SERVICE_TYPE_INSPECTION, 365); // 1 year
    }
    
    // Mileage interval recommendations
    private static final Map<String, Integer> MILEAGE_INTERVALS = new HashMap<>();
    static {
        MILEAGE_INTERVALS.put(SERVICE_TYPE_OIL_CHANGE, 5000);
        MILEAGE_INTERVALS.put(SERVICE_TYPE_TIRE_ROTATION, 7500);
        MILEAGE_INTERVALS.put(SERVICE_TYPE_BRAKE_SERVICE, 30000);
        MILEAGE_INTERVALS.put(SERVICE_TYPE_ALIGNMENT, 20000);
    }
    
    public VehicleServiceHistoryService(ServiceRecordDao serviceRecordDao, 
                                       EmailService emailService,
                                       SettingsService settingsService) {
        this.serviceRecordDao = serviceRecordDao;
        this.emailService = emailService;
        this.settingsService = settingsService;
    }
    
    /**
     * Create a service record from a completed sale
     */
    public ServiceRecord createServiceRecordFromSale(Sale sale) {
        if (sale.getVehicle() == null) {
            return null; // No vehicle associated with sale
        }
        
        ServiceRecord record = new ServiceRecord();
        record.setVehicle(sale.getVehicle());
        record.setSale(sale);
        record.setServiceDate(sale.getTimestamp());
        record.setTotalCost(sale.getTotal());
        
        // Determine service type based on items
        String serviceType = determineServiceType(sale);
        record.setServiceType(serviceType);
        
        // Build description from sale items
        StringBuilder description = new StringBuilder();
        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() != null) {
                ServiceRecord.ServiceItem serviceItem = new ServiceRecord.ServiceItem();
                serviceItem.setItemType("PRODUCT");
                serviceItem.setItemName(item.getProduct().getName());
                serviceItem.setQuantity(item.getQuantity());
                serviceItem.setUnitPrice(item.getUnitPrice());
                serviceItem.setPartNumber(item.getProduct().getSku());
                record.addServiceItem(serviceItem);
                
                description.append(item.getProduct().getName())
                          .append(" (Qty: ").append(item.getQuantity()).append("), ");
            }
            if (item.getService() != null) {
                ServiceRecord.ServiceItem serviceItem = new ServiceRecord.ServiceItem();
                serviceItem.setItemType("SERVICE");
                serviceItem.setItemName(item.getService().getName());
                serviceItem.setQuantity(item.getQuantity());
                serviceItem.setUnitPrice(item.getUnitPrice());
                record.addServiceItem(serviceItem);
                
                description.append(item.getService().getName()).append(", ");
            }
        }
        
        if (description.length() > 2) {
            description.setLength(description.length() - 2); // Remove trailing comma
        }
        record.setDescription(description.toString());
        
        // Set technician if available
        if (!sale.getItems().isEmpty() && sale.getItems().get(0).getTechnician() != null) {
            record.setTechnician(sale.getItems().get(0).getTechnician());
        }
        
        // Calculate next service date/mileage
        calculateNextService(record);
        
        // Save the record
        return serviceRecordDao.save(record);
    }
    
    /**
     * Get complete service history for a vehicle
     */
    public List<ServiceRecord> getVehicleServiceHistory(Long vehicleId) {
        return serviceRecordDao.findByVehicleId(vehicleId);
    }
    
    /**
     * Get last service date for a specific service type
     */
    public LocalDateTime getLastServiceDate(Long vehicleId, String serviceType) {
        List<ServiceRecord> records = serviceRecordDao.findByVehicleId(vehicleId);
        return records.stream()
            .filter(r -> serviceType.equals(r.getServiceType()))
            .map(ServiceRecord::getServiceDate)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get service recommendations for a vehicle
     */
    public List<ServiceRecommendation> getServiceRecommendations(Long vehicleId, Integer currentMileage) {
        List<ServiceRecommendation> recommendations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Check each service type
        for (Map.Entry<String, Integer> entry : SERVICE_INTERVALS.entrySet()) {
            String serviceType = entry.getKey();
            Integer dayInterval = entry.getValue();
            
            LocalDateTime lastServiceDate = getLastServiceDate(vehicleId, serviceType);
            
            // Check time-based recommendation
            if (lastServiceDate == null || 
                lastServiceDate.plusDays(dayInterval).isBefore(now)) {
                
                ServiceRecommendation rec = new ServiceRecommendation();
                rec.setServiceType(serviceType);
                rec.setReason("Time-based interval");
                rec.setUrgency(calculateUrgency(lastServiceDate, dayInterval));
                rec.setLastServiceDate(lastServiceDate);
                recommendations.add(rec);
            }
            
            // Check mileage-based recommendation if applicable
            if (currentMileage != null && MILEAGE_INTERVALS.containsKey(serviceType)) {
                ServiceRecord lastRecord = getLastServiceRecord(vehicleId, serviceType);
                if (lastRecord != null && lastRecord.getMileage() != null) {
                    int mileageSinceService = currentMileage - lastRecord.getMileage();
                    if (mileageSinceService >= MILEAGE_INTERVALS.get(serviceType)) {
                        ServiceRecommendation rec = new ServiceRecommendation();
                        rec.setServiceType(serviceType);
                        rec.setReason("Mileage-based interval");
                        rec.setUrgency("HIGH");
                        rec.setLastServiceDate(lastServiceDate);
                        rec.setMileageSinceService(mileageSinceService);
                        recommendations.add(rec);
                    }
                }
            }
        }
        
        return recommendations;
    }
    
    /**
     * Send service reminder emails
     */
    public void sendServiceReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<ServiceRecord> dueForService = serviceRecordDao.findVehiclesDueForService(now.plusDays(7));
        
        for (ServiceRecord record : dueForService) {
            if (record.getVehicle().getCustomer() != null && 
                record.getVehicle().getCustomer().getEmail() != null) {
                
                String subject = "Service Reminder - " + settingsService.getCompanyName();
                String body = buildServiceReminderEmail(record);
                
                // Send email using existing EmailService
                // You might want to add a sendServiceReminder method to EmailService
            }
        }
    }
    
    /**
     * Update service record with actual work performed
     */
    public ServiceRecord updateServiceRecord(Long recordId, Integer mileage, String notes,
                                           List<ServiceRecord.ServiceItem> items) {
        Optional<ServiceRecord> recordOpt = serviceRecordDao.findById(recordId);
        if (recordOpt.isPresent()) {
            ServiceRecord record = recordOpt.get();
            if (mileage != null) {
                record.setMileage(mileage);
            }
            if (notes != null) {
                record.setNotes(notes);
            }
            if (items != null) {
                record.setServiceItems(items);
            }
            calculateNextService(record);
            return serviceRecordDao.update(record);
        }
        return null;
    }
    
    /**
     * Get vehicles due for service
     */
    public List<VehicleServiceDue> getVehiclesDueForService() {
        LocalDateTime now = LocalDateTime.now();
        List<ServiceRecord> dueRecords = serviceRecordDao.findVehiclesDueForService(now.plusDays(30));
        
        Map<Vehicle, List<ServiceRecord>> vehicleMap = dueRecords.stream()
            .collect(Collectors.groupingBy(ServiceRecord::getVehicle));
        
        List<VehicleServiceDue> result = new ArrayList<>();
        for (Map.Entry<Vehicle, List<ServiceRecord>> entry : vehicleMap.entrySet()) {
            VehicleServiceDue due = new VehicleServiceDue();
            due.setVehicle(entry.getKey());
            due.setDueServices(entry.getValue().stream()
                .map(ServiceRecord::getServiceType)
                .collect(Collectors.toList()));
            due.setEarliestDueDate(entry.getValue().stream()
                .map(ServiceRecord::getNextServiceDate)
                .min(LocalDateTime::compareTo)
                .orElse(null));
            result.add(due);
        }
        
        return result;
    }
    
    // Helper methods
    
    private String determineServiceType(Sale sale) {
        boolean hasTires = false;
        boolean hasOilChange = false;
        boolean hasBrakeService = false;
        
        for (SaleItem item : sale.getItems()) {
            if (item.getProduct() != null && "TIRE".equalsIgnoreCase(item.getProduct().getCategory())) {
                hasTires = true;
            }
            if (item.getService() != null) {
                String serviceName = item.getService().getName().toLowerCase();
                if (serviceName.contains("oil")) {
                    hasOilChange = true;
                } else if (serviceName.contains("brake")) {
                    hasBrakeService = true;
                }
            }
        }
        
        if (hasTires) return SERVICE_TYPE_TIRE_REPLACEMENT;
        if (hasOilChange) return SERVICE_TYPE_OIL_CHANGE;
        if (hasBrakeService) return SERVICE_TYPE_BRAKE_SERVICE;
        
        return SERVICE_TYPE_OTHER;
    }
    
    private void calculateNextService(ServiceRecord record) {
        String serviceType = record.getServiceType();
        
        // Set next service date
        if (SERVICE_INTERVALS.containsKey(serviceType)) {
            int days = SERVICE_INTERVALS.get(serviceType);
            record.setNextServiceDate(record.getServiceDate().plusDays(days));
        }
        
        // Set next service mileage
        if (record.getMileage() != null && MILEAGE_INTERVALS.containsKey(serviceType)) {
            int miles = MILEAGE_INTERVALS.get(serviceType);
            record.setNextServiceMileage(record.getMileage() + miles);
        }
    }
    
    private ServiceRecord getLastServiceRecord(Long vehicleId, String serviceType) {
        List<ServiceRecord> records = serviceRecordDao.findByVehicleId(vehicleId);
        return records.stream()
            .filter(r -> serviceType.equals(r.getServiceType()))
            .findFirst()
            .orElse(null);
    }
    
    private String calculateUrgency(LocalDateTime lastServiceDate, Integer dayInterval) {
        if (lastServiceDate == null) return "MEDIUM";
        
        LocalDateTime dueDate = lastServiceDate.plusDays(dayInterval);
        LocalDateTime now = LocalDateTime.now();
        
        if (dueDate.isBefore(now)) return "HIGH";
        if (dueDate.isBefore(now.plusDays(30))) return "MEDIUM";
        return "LOW";
    }
    
    private String buildServiceReminderEmail(ServiceRecord record) {
        return String.format(
            "Dear %s,\n\n" +
            "Your %s %s %s is due for the following service:\n\n" +
            "Service Type: %s\n" +
            "Last Service Date: %s\n" +
            "Recommended Service Date: %s\n\n" +
            "Please schedule an appointment at your earliest convenience.\n\n" +
            "Thank you,\n%s",
            record.getVehicle().getCustomer().getFirstName(),
            record.getVehicle().getModelYear(),
            record.getVehicle().getMake(),
            record.getVehicle().getModel(),
            record.getServiceType(),
            record.getServiceDate().toLocalDate(),
            record.getNextServiceDate() != null ? record.getNextServiceDate().toLocalDate() : "As soon as possible",
            settingsService.getCompanyName()
        );
    }
    
    // Inner classes for recommendations
    
    public static class ServiceRecommendation {
        private String serviceType;
        private String reason;
        private String urgency;
        private LocalDateTime lastServiceDate;
        private Integer mileageSinceService;
        
        // Getters and setters
        public String getServiceType() { return serviceType; }
        public void setServiceType(String serviceType) { this.serviceType = serviceType; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getUrgency() { return urgency; }
        public void setUrgency(String urgency) { this.urgency = urgency; }
        
        public LocalDateTime getLastServiceDate() { return lastServiceDate; }
        public void setLastServiceDate(LocalDateTime lastServiceDate) { this.lastServiceDate = lastServiceDate; }
        
        public Integer getMileageSinceService() { return mileageSinceService; }
        public void setMileageSinceService(Integer mileageSinceService) { this.mileageSinceService = mileageSinceService; }
    }
    
    public static class VehicleServiceDue {
        private Vehicle vehicle;
        private List<String> dueServices;
        private LocalDateTime earliestDueDate;
        
        // Getters and setters
        public Vehicle getVehicle() { return vehicle; }
        public void setVehicle(Vehicle vehicle) { this.vehicle = vehicle; }
        
        public List<String> getDueServices() { return dueServices; }
        public void setDueServices(List<String> dueServices) { this.dueServices = dueServices; }
        
        public LocalDateTime getEarliestDueDate() { return earliestDueDate; }
        public void setEarliestDueDate(LocalDateTime earliestDueDate) { this.earliestDueDate = earliestDueDate; }
    }
} 