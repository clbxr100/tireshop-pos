package com.tireshop.util;

import com.tireshop.model.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to initialize database with sample data
 */
public class DatabaseInitializer {
    
    /**
     * Initialize the database with sample data
     * @param sessionFactory Hibernate session factory
     */
    public static void initialize(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        
        try {
            // DISABLED - No mock data will be created
            System.out.println("Sample data creation disabled - starting with clean database.");
            /*
            // Create sample customers
            List<Customer> customers = createSampleCustomers(session);
            
            // Create sample vehicles
            List<Vehicle> vehicles = createSampleVehicles(session, customers);
            
            // Create sample products
            List<Product> products = createSampleProducts(session);
            
            // Create sample technicians
            List<Technician> technicians = createSampleTechnicians(session);
            
            // Create sample services
            List<Service> services = createSampleServices(session);
            
            // Create sample sales
            createSampleSales(session, customers, vehicles, products, services);
            
            // Uncomment appointments creation
            createSampleAppointments(session, customers, vehicles, technicians);
            */
            
            tx.commit();
            System.out.println("Sample data initialization completed successfully.");
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            System.err.println("Error initializing sample data: " + e.getMessage());
            e.printStackTrace();
        } finally {
            session.close();
        }
    }
    
    private static List<Customer> createSampleCustomers(Session session) {
        List<Customer> customers = new ArrayList<>();
        
        // Create sample customers
        Customer customer1 = new Customer();
        customer1.setFirstName("John");
        customer1.setLastName("Doe");
        customer1.setPhone("555-123-4567");
        customer1.setEmail("john.doe@example.com");
        customer1.setAddress("123 Main St");
        session.save(customer1);
        customers.add(customer1);
        
        Customer customer2 = new Customer();
        customer2.setFirstName("Jane");
        customer2.setLastName("Smith");
        customer2.setPhone("555-987-6543");
        customer2.setEmail("jane.smith@example.com");
        customer2.setAddress("456 Oak Ave");
        session.save(customer2);
        customers.add(customer2);
        
        Customer customer3 = new Customer();
        customer3.setFirstName("Bob");
        customer3.setLastName("Johnson");
        customer3.setPhone("555-555-5555");
        customer3.setEmail("bob.johnson@example.com");
        customer3.setAddress("789 Pine Blvd");
        session.save(customer3);
        customers.add(customer3);
        
        return customers;
    }
    
    private static List<Vehicle> createSampleVehicles(Session session, List<Customer> customers) {
        List<Vehicle> vehicles = new ArrayList<>();
        
        // Create sample vehicles
        Vehicle vehicle1 = new Vehicle();
        vehicle1.setMake("Honda");
        vehicle1.setModel("Accord");
        vehicle1.setModelYear(2018);
        vehicle1.setVin("1HGCV1F18JA123456");
        vehicle1.setLicensePlate("ABC123");
        vehicle1.setColor("Black");
        
        // Use Customer.addVehicle to properly associate the vehicle with the customer
        customers.get(0).addVehicle(vehicle1);
        session.save(vehicle1);
        vehicles.add(vehicle1);
        
        Vehicle vehicle2 = new Vehicle();
        vehicle2.setMake("Toyota");
        vehicle2.setModel("Camry");
        vehicle2.setModelYear(2020);
        vehicle2.setVin("4T1BF1FK9GU123456");
        vehicle2.setLicensePlate("XYZ789");
        vehicle2.setColor("Silver");
        
        // Use Customer.addVehicle to properly associate the vehicle with the customer
        customers.get(1).addVehicle(vehicle2);
        session.save(vehicle2);
        vehicles.add(vehicle2);
        
        Vehicle vehicle3 = new Vehicle();
        vehicle3.setMake("Ford");
        vehicle3.setModel("F-150");
        vehicle3.setModelYear(2019);
        vehicle3.setVin("1FTEW1EP7JFB12345");
        vehicle3.setLicensePlate("DEF456");
        vehicle3.setColor("Red");
        
        // Use Customer.addVehicle to properly associate the vehicle with the customer
        customers.get(2).addVehicle(vehicle3);
        session.save(vehicle3);
        vehicles.add(vehicle3);
        
        return vehicles;
    }
    
    private static List<Product> createSampleProducts(Session session) {
        List<Product> products = new ArrayList<>();
        
        // Create sample products
        Product product1 = new Product();
        product1.setName("Michelin Defender T+H");
        product1.setDescription("All-season passenger tire");
        product1.setBarcode("MICH-DEF-001");
        product1.setSellingPrice(new BigDecimal("120.00"));
        product1.setPurchasePrice(new BigDecimal("85.00"));
        product1.setQuantityInStock(20);
        product1.setCategory("Tires");
        product1.setReorderLevel(10);
        session.save(product1);
        products.add(product1);
        
        Product product2 = new Product();
        product2.setName("Goodyear Assurance WeatherReady");
        product2.setDescription("Premium all-weather tire");
        product2.setBarcode("GOOD-AWR-002");
        product2.setSellingPrice(new BigDecimal("150.00"));
        product2.setPurchasePrice(new BigDecimal("105.00"));
        product2.setQuantityInStock(15);
        product2.setCategory("Tires");
        product2.setReorderLevel(5);
        session.save(product2);
        products.add(product2);
        
        Product product3 = new Product();
        product3.setName("Continental ExtremeContact DWS 06");
        product3.setDescription("Ultra high performance all-season tire");
        product3.setBarcode("CONT-DWS-003");
        product3.setSellingPrice(new BigDecimal("180.00"));
        product3.setPurchasePrice(new BigDecimal("130.00"));
        product3.setQuantityInStock(10);
        product3.setCategory("Tires");
        product3.setReorderLevel(8);
        session.save(product3);
        products.add(product3);
        
        // Add some sample products
        Product p1 = new Product();
        p1.setName("Mastercraft Winter Tire 215/55R17");
        p1.setDescription("Premium winter tire with excellent grip");
        p1.setPrice(new BigDecimal("129.99"));
        p1.setSellingPrice(new BigDecimal("129.99"));
        p1.setQuantityInStock(20);
        p1.setReorderLevel(5);
        p1.setCategory("Tire");
        p1.setBarcode("1234567890");
        p1.setSize("215/55R17");
        session.save(p1);
        
        Product p2 = new Product();
        p2.setName("Goodyear All-Season Tire 205/65R16");
        p2.setDescription("Reliable all-season performance");
        p2.setPrice(new BigDecimal("99.99"));
        p2.setSellingPrice(new BigDecimal("99.99"));
        p2.setQuantityInStock(15);
        p2.setReorderLevel(4);
        p2.setCategory("Tire");
        p2.setBarcode("3456789012");
        p2.setSize("205/65R16");
        session.save(p2);
        
        Product p3 = new Product();
        p3.setName("Bridgestone Performance Tire 225/45R18");
        p3.setDescription("High-performance tire for sports vehicles");
        p3.setPrice(new BigDecimal("159.99"));
        p3.setSellingPrice(new BigDecimal("159.99"));
        p3.setQuantityInStock(8);
        p3.setReorderLevel(3);
        p3.setCategory("Tire");
        p3.setBarcode("5678901234");
        p3.setSize("225/45R18");
        session.save(p3);
        
        Product p4 = new Product();
        p4.setName("Premium Alloy Rim 18\"");
        p4.setDescription("Lightweight alloy rim");
        p4.setPrice(new BigDecimal("199.99"));
        p4.setSellingPrice(new BigDecimal("199.99"));
        p4.setQuantityInStock(10);
        p4.setReorderLevel(2);
        p4.setCategory("Rim");
        session.save(p4);
        
        // Add specific Mastercraft tire with real barcode
        Product mastercraft73825 = new Product();
        mastercraft73825.setName("Mastercraft Courser AXT2 275/65R18");
        mastercraft73825.setDescription("Mastercraft Courser AXT2 All-Terrain Truck Tire - Size 275/65R18");
        mastercraft73825.setPrice(new BigDecimal("189.99"));
        mastercraft73825.setSellingPrice(new BigDecimal("189.99"));
        mastercraft73825.setQuantityInStock(8);
        mastercraft73825.setReorderLevel(4);
        mastercraft73825.setCategory("Tire");
        mastercraft73825.setManufacturer("Mastercraft");
        
        // Store with various formats to ensure it can be found
        mastercraft73825.setBarcode("029142738251"); // Standard UPC format with one leading zero
        System.out.println("Added Mastercraft tire with barcode: 029142738251");
        
        // Also add with just digits (no leading zero) to test
        Product mastercraftNoZero = new Product();
        mastercraftNoZero.setName("Mastercraft Courser AXT2 275/65R18 (No Leading Zero)");
        mastercraftNoZero.setDescription("Mastercraft Courser AXT2 All-Terrain Truck Tire - Size 275/65R18");
        mastercraftNoZero.setPrice(new BigDecimal("189.99"));
        mastercraftNoZero.setSellingPrice(new BigDecimal("189.99"));
        mastercraftNoZero.setQuantityInStock(5);
        mastercraftNoZero.setReorderLevel(4);
        mastercraftNoZero.setCategory("Tire");
        mastercraftNoZero.setManufacturer("Mastercraft");
        mastercraftNoZero.setBarcode("29142738251"); // No leading zero version
        System.out.println("Added Mastercraft tire with barcode: 29142738251 (no leading zero)");
        
        mastercraft73825.setSize("275/65R18");
        mastercraft73825.setModelNumber("73825");
        session.save(mastercraft73825);
        
        mastercraftNoZero.setSize("275/65R18");
        mastercraftNoZero.setModelNumber("73825");
        session.save(mastercraftNoZero);
        
        // Add Mastercraft 28038 model (the one the user scanned)
        Product mastercraft28038 = new Product();
        mastercraft28038.setName("Mastercraft Courser HSX 235/65R17");
        mastercraft28038.setDescription("Mastercraft Courser HSX Highway All-Season Tire - Size 235/65R17 104T");
        mastercraft28038.setPrice(new BigDecimal("159.99"));
        mastercraft28038.setSellingPrice(new BigDecimal("159.99"));
        mastercraft28038.setQuantityInStock(12);
        mastercraft28038.setReorderLevel(4);
        mastercraft28038.setCategory("Tire");
        mastercraft28038.setManufacturer("Mastercraft");
        mastercraft28038.setBarcode("029142803881");
        mastercraft28038.setSize("235/65R17");
        mastercraft28038.setModelNumber("28038");
        mastercraft28038.setLocation("Aisle B, Rack 3");
        mastercraft28038.setPurchasePrice(new BigDecimal("109.99"));
        System.out.println("Added Mastercraft tire model 28038 with barcode: 029142803881");
        session.save(mastercraft28038);
        
        return products;
    }
    
    private static List<Service> createSampleServices(Session session) {
        List<Service> services = new ArrayList<>();
        
        // Tire Services
        Service service1 = new Service();
        service1.setName("Tire Mounting & Balancing");
        service1.setDescription("Remove old tire, mount new tire, balance wheel assembly");
        service1.setPrice(new BigDecimal("25.00"));
        service1.setEstimatedDurationMinutes(30);
        service1.setCategory("Tire Service");
        session.save(service1);
        services.add(service1);
        
        Service service2 = new Service();
        service2.setName("Tire Rotation");
        service2.setDescription("Rotate tires to ensure even wear and extend tire life");
        service2.setPrice(new BigDecimal("20.00"));
        service2.setEstimatedDurationMinutes(20);
        service2.setCategory("Tire Service");
        session.save(service2);
        services.add(service2);
        
        Service service3 = new Service();
        service3.setName("Wheel Alignment");
        service3.setDescription("Adjust wheel angles to manufacturer specifications");
        service3.setPrice(new BigDecimal("89.99"));
        service3.setEstimatedDurationMinutes(60);
        service3.setCategory("Wheel Service");
        session.save(service3);
        services.add(service3);
        
        return services;
    }
    
    private static List<Technician> createSampleTechnicians(Session session) {
        List<Technician> technicians = new ArrayList<>();
        
        Technician tech1 = new Technician();
        tech1.setFirstName("Mike");
        tech1.setLastName("Wilson");
        tech1.setEmployeeId("T001");
        tech1.setSpecialization("Tire Specialist");
        tech1.setActive(true);
        session.save(tech1);
        technicians.add(tech1);
        
        Technician tech2 = new Technician();
        tech2.setFirstName("Sarah");
        tech2.setLastName("Johnson");
        tech2.setEmployeeId("T002");
        tech2.setSpecialization("Alignment Specialist");
        tech2.setActive(true);
        session.save(tech2);
        technicians.add(tech2);
        
        return technicians;
    }
    
    private static void createSampleSales(Session session, List<Customer> customers, 
                                          List<Vehicle> vehicles, List<Product> products, 
                                          List<Service> services) {
        // Create a sample sale
        Sale sale1 = new Sale();
        sale1.setInvoiceNumber("INV-20230101-123ABC");
        sale1.setTimestamp(LocalDateTime.now().minusDays(1));
        sale1.setCustomer(customers.get(0));
        sale1.setVehicle(vehicles.get(0));
        
        // Add items to the sale
        SaleItem item1 = new SaleItem();
        item1.setProduct(products.get(0));
        item1.setQuantity(4);
        item1.setUnitPrice(products.get(0).getSellingPrice());
        item1.setSale(sale1);
        session.save(item1);
        
        sale1.getItems().add(item1);
        sale1.recalculateAmounts();
        
        // Set payment info
        sale1.setPaymentType(PaymentType.CREDIT_CARD);
        sale1.setCardType("Visa");
        sale1.setCardLastFour("1234");
        sale1.setAuthorizationCode("AUTH123");
        sale1.setPaid(true);
        
        session.save(sale1);
    }
    
    private static void createSampleAppointments(Session session, List<Customer> customers, 
                                               List<Vehicle> vehicles, List<Technician> technicians) {
        // Create sample appointments for today
        LocalDate today = LocalDate.now();
        
        // Appointment 1 - Morning
        Appointment appointment1 = new Appointment();
        appointment1.setTitle("Tire Rotation & Balance");
        appointment1.setDescription("Customer requested rotation and balancing of all 4 tires");
        appointment1.setStartTime(today.atTime(10, 0)); // 10:00 AM
        appointment1.setEndTime(today.atTime(11, 0));  // 11:00 AM
        appointment1.setCustomer(customers.get(0));
        appointment1.setVehicle(vehicles.get(0));
        appointment1.setTechnician(technicians.get(0));
        appointment1.setStatus(AppointmentStatus.CONFIRMED);
        session.save(appointment1);
        
        // Appointment 2 - Afternoon
        Appointment appointment2 = new Appointment();
        appointment2.setTitle("New Tire Installation");
        appointment2.setDescription("Install 4 new Michelin tires");
        appointment2.setStartTime(today.atTime(13, 30)); // 1:30 PM 
        appointment2.setEndTime(today.atTime(15, 0));   // 3:00 PM
        appointment2.setCustomer(customers.get(1));
        appointment2.setVehicle(vehicles.get(1));
        appointment2.setTechnician(technicians.get(1));
        appointment2.setStatus(AppointmentStatus.SCHEDULED);
        session.save(appointment2);
        
        // Appointment 3 - Later today
        Appointment appointment3 = new Appointment();
        appointment3.setTitle("Wheel Alignment");
        appointment3.setDescription("Customer reports pulling to the right");
        appointment3.setStartTime(today.atTime(16, 0)); // 4:00 PM
        appointment3.setEndTime(today.atTime(17, 0));  // 5:00 PM
        appointment3.setCustomer(customers.get(2));
        appointment3.setVehicle(vehicles.get(2));
        appointment3.setTechnician(technicians.get(0));
        appointment3.setStatus(AppointmentStatus.SCHEDULED);
        session.save(appointment3);
        
        // Appointment 4 - Tomorrow
        Appointment appointment4 = new Appointment();
        appointment4.setTitle("Flat Repair");
        appointment4.setDescription("Repair puncture in right rear tire");
        appointment4.setStartTime(today.plusDays(1).atTime(9, 0)); // 9:00 AM tomorrow
        appointment4.setEndTime(today.plusDays(1).atTime(10, 0));  // 10:00 AM tomorrow
        appointment4.setCustomer(customers.get(0));
        appointment4.setVehicle(vehicles.get(0));
        appointment4.setTechnician(technicians.get(1));
        appointment4.setStatus(AppointmentStatus.SCHEDULED);
        session.save(appointment4);
    }
} 