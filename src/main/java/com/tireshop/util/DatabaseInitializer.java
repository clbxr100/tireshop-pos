package com.tireshop.util;

import com.tireshop.dao.UserDao;
import com.tireshop.model.*;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.UUID;

/**
 * Helper class to initialize database with sample data
 */
public class DatabaseInitializer {
    
    private static final Logger LOGGER = Logger.getLogger(DatabaseInitializer.class.getName());
    
    /**
     * Initialize the database with sample data
     * @param sessionFactory Hibernate session factory
     */
    public static void initialize(SessionFactory sessionFactory) {
        List<Customer> customers = null;
        List<Vehicle> vehicles = null;
        List<Technician> technicians = null;
        List<Service> services = null;
        List<Product> products = null;
        
        try {
            // Check if customers exist to determine if it's a fresh DB for extensive sample data
            boolean loadExtensiveSampleData = false;
            try (Session checkSession = sessionFactory.openSession()) {
                long customerCount = (Long) checkSession.createQuery("select count(c.id) from Customer c").uniqueResult();
                if (customerCount == 0) {
                    loadExtensiveSampleData = true;
                    LOGGER.info("No existing customers found, proceeding with extensive sample data initialization.");
                } else {
                    LOGGER.info(customerCount + " existing customers found, skipping extensive sample data initialization.");
                }
            } catch (Exception e) {
                // If there's an error (e.g., tables don't exist yet), assume fresh DB
                LOGGER.warning("Could not check for existing customers, assuming fresh DB for extensive sample data. Error: " + e.getMessage());
                loadExtensiveSampleData = true; 
            }

            if (loadExtensiveSampleData) {
                // DISABLED - No mock data will be created
                LOGGER.info("Sample data creation disabled - starting with clean database.");
                /*
                // Create sample customers
                customers = initializeCustomers(sessionFactory);
                LOGGER.info("Customers initialized successfully.");
                
                // Create sample vehicles
                vehicles = initializeVehicles(sessionFactory, customers);
                LOGGER.info("Vehicles initialized successfully.");
                
                // Create sample technicians
                technicians = initializeTechnicians(sessionFactory);
                LOGGER.info("Technicians initialized successfully.");
                
                // Create sample services
                services = initializeServices(sessionFactory);
                LOGGER.info("Services initialized successfully.");
                
                // Create sample products
                products = initializeProducts(sessionFactory);
                LOGGER.info("Products initialized successfully.");
                
                // Create sample sales
                initializeSales(sessionFactory, customers, vehicles, products, services);
                LOGGER.info("Sales initialized successfully.");
                
                // Create sample appointments
                initializeAppointments(sessionFactory, customers, vehicles, technicians);
                LOGGER.info("Appointments initialized successfully.");
                */
            }
            
            // Initialize default admin user (always attempt this, as it has its own internal check)
            UserDao userDao = new UserDao(sessionFactory);
            if (userDao.findByUsername("admin").isEmpty()) {
                System.out.println("DB Initializer: Default admin user not found. Creating...");
                User adminUser = new User(
                    UUID.randomUUID().toString(), // Generate a new UUID for the ID
                    "admin", 
                    "admin123", // Plain text password for simplicity
                    "ADMIN", 
                    true
                );
                try {
                    userDao.saveOrUpdateUser(adminUser);
                    System.out.println("DB Initializer: Default admin user created successfully.");
                } catch (Exception e) {
                    System.err.println("DB Initializer: Failed to create default admin user: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.out.println("DB Initializer: Default admin user already exists.");
            }
            
            LOGGER.info("Sample data initialization completed successfully.");
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error initializing sample data", e);
            throw new RuntimeException("Error initializing sample data", e);
        }
    }
    
    private static List<Customer> initializeCustomers(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Customer> customers = new ArrayList<>();
        
        try {
            Customer customer1 = new Customer();
            customer1.setFirstName("John");
            customer1.setLastName("Smith");
            customer1.setEmail("john.smith@email.com");
            customer1.setPhone("555-0123");
            customer1.setAddress("123 Main St");
            session.save(customer1);
            customers.add(customer1);
            
            Customer customer2 = new Customer();
            customer2.setFirstName("Jane");
            customer2.setLastName("Doe");
            customer2.setEmail("jane.doe@email.com");
            customer2.setPhone("555-0124");
            customer2.setAddress("456 Oak Ave");
            session.save(customer2);
            customers.add(customer2);
            
            Customer customer3 = new Customer();
            customer3.setFirstName("Bob");
            customer3.setLastName("Johnson");
            customer3.setEmail("bob.johnson@email.com");
            customer3.setPhone("555-0125");
            customer3.setAddress("789 Pine Rd");
            session.save(customer3);
            customers.add(customer3);
            
            tx.commit();
            return customers;
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static List<Vehicle> initializeVehicles(SessionFactory sessionFactory, List<Customer> customers) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Vehicle> vehicles = new ArrayList<>();
        
        try {
            Vehicle vehicle1 = new Vehicle();
            vehicle1.setMake("Toyota");
            vehicle1.setModel("Camry");
            vehicle1.setModelYear(2020);
            vehicle1.setColor("Silver");
            vehicle1.setLicensePlate("ABC123");
            vehicle1.setVin("1HGCM82633A123456");
            vehicle1.setCustomer(customers.get(0));
            session.save(vehicle1);
            vehicles.add(vehicle1);
            
            Vehicle vehicle2 = new Vehicle();
            vehicle2.setMake("Honda");
            vehicle2.setModel("Accord");
            vehicle2.setModelYear(2019);
            vehicle2.setColor("Black");
            vehicle2.setLicensePlate("XYZ789");
            vehicle2.setVin("2HGES16575H123456");
            vehicle2.setCustomer(customers.get(1));
            session.save(vehicle2);
            vehicles.add(vehicle2);
            
            Vehicle vehicle3 = new Vehicle();
            vehicle3.setMake("Ford");
            vehicle3.setModel("F-150");
            vehicle3.setModelYear(2021);
            vehicle3.setColor("Red");
            vehicle3.setLicensePlate("DEF456");
            vehicle3.setVin("1FTEW1EG5JFB12345");
            vehicle3.setCustomer(customers.get(2));
            session.save(vehicle3);
            vehicles.add(vehicle3);
            
            tx.commit();
            return vehicles;
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static List<Product> initializeProducts(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Product> products = new ArrayList<>();
        
        try {
            // Premium All-Season Tire
            Product tire1 = new Product();
            tire1.setName("Premium All-Season Tire");
            tire1.setDescription("High-performance all-season tire with excellent grip");
            tire1.setManufacturer("Michelin");
            tire1.setSize("225/45R17");
            tire1.setTireType("All-Season");
            tire1.setSpeedRating("H");
            tire1.setLoadRating("94");
            tire1.setUtqgTreadwear(500);
            tire1.setUtqgTraction("A");
            tire1.setUtqgTemperature("A");
            tire1.setPrice(new BigDecimal("199.99"));
            tire1.setSellingPrice(new BigDecimal("249.99"));
            tire1.setQuantityInStock(10);
            tire1.setReorderLevel(5);
            tire1.setBarcode("123456789");
            session.save(tire1);
            products.add(tire1);
            
            // Winter Performance Tire
            Product tire2 = new Product();
            tire2.setName("Winter Performance Tire");
            tire2.setDescription("Superior winter performance and handling");
            tire2.setManufacturer("Bridgestone");
            tire2.setSize("205/55R16");
            tire2.setTireType("Winter");
            tire2.setSpeedRating("T");
            tire2.setLoadRating("91");
            tire2.setUtqgTreadwear(400);
            tire2.setUtqgTraction("AA");
            tire2.setUtqgTemperature("B");
            tire2.setPrice(new BigDecimal("179.99"));
            tire2.setSellingPrice(new BigDecimal("229.99"));
            tire2.setQuantityInStock(8);
            tire2.setReorderLevel(4);
            tire2.setBarcode("987654321");
            session.save(tire2);
            products.add(tire2);
            
            // All-Terrain Tire
            Product tire3 = new Product();
            tire3.setName("All-Terrain Tire");
            tire3.setDescription("Rugged all-terrain performance");
            tire3.setManufacturer("Goodyear");
            tire3.setSize("265/70R17");
            tire3.setTireType("All-Terrain");
            tire3.setSpeedRating("S");
            tire3.setLoadRating("115");
            tire3.setUtqgTreadwear(450);
            tire3.setUtqgTraction("A");
            tire3.setUtqgTemperature("B");
            tire3.setPrice(new BigDecimal("219.99"));
            tire3.setSellingPrice(new BigDecimal("279.99"));
            tire3.setQuantityInStock(6);
            tire3.setReorderLevel(3);
            tire3.setBarcode("456789123");
            session.save(tire3);
            products.add(tire3);
            
            tx.commit();
            return products;
            
        } catch (ConstraintViolationException e) {
            if (tx != null) tx.rollback();
            LOGGER.warning("Skipping duplicate product: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static List<Technician> initializeTechnicians(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Technician> technicians = new ArrayList<>();
        
        try {
            Technician tech1 = new Technician();
            tech1.setFirstName("Mike");
            tech1.setLastName("Wilson");
            tech1.setEmployeeId("T001");
            tech1.setSpecialization("General Service");
            tech1.setActive(true);
            session.save(tech1);
            technicians.add(tech1);
            
            Technician tech2 = new Technician();
            tech2.setFirstName("Sarah");
            tech2.setLastName("Brown");
            tech2.setEmployeeId("T002");
            tech2.setSpecialization("Alignment Specialist");
            tech2.setActive(true);
            session.save(tech2);
            technicians.add(tech2);
            
            tx.commit();
            return technicians;
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static List<Service> initializeServices(SessionFactory sessionFactory) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        List<Service> services = new ArrayList<>();
        
        try {
            Service service1 = new Service();
            service1.setName("Tire Rotation");
            service1.setDescription("Rotate tires to ensure even wear");
            service1.setPrice(new BigDecimal("29.99"));
            service1.setEstimatedDurationMinutes(30);
            service1.setCategory("Maintenance");
            session.save(service1);
            services.add(service1);
            
            Service service2 = new Service();
            service2.setName("Wheel Alignment");
            service2.setDescription("Four-wheel alignment service");
            service2.setPrice(new BigDecimal("89.99"));
            service2.setEstimatedDurationMinutes(60);
            service2.setCategory("Alignment");
            session.save(service2);
            services.add(service2);
            
            Service service3 = new Service();
            service3.setName("Tire Installation");
            service3.setDescription("Mount and balance new tires");
            service3.setPrice(new BigDecimal("20.00"));
            service3.setEstimatedDurationMinutes(45);
            service3.setCategory("Installation");
            session.save(service3);
            services.add(service3);
            
            tx.commit();
            return services;
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static void initializeSales(SessionFactory sessionFactory, List<Customer> customers, 
            List<Vehicle> vehicles, List<Product> products, List<Service> services) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        
        try {
            // Create a sample sale
            Sale sale = new Sale();
            sale.setCustomer(customers.get(0));
            sale.setVehicle(vehicles.get(0));
            sale.setTimestamp(LocalDateTime.now());
            sale.setSubtotal(new BigDecimal("279.99"));
            sale.setTax(new BigDecimal("22.40"));
            sale.setTotal(new BigDecimal("302.39"));
            sale.setPaymentType(PaymentType.CREDIT_CARD);
            sale.setCardType("Visa");
            sale.setCardLastFour("1234");
            sale.setAuthorizationCode("AUTH123");
            sale.setPaid(true);
            session.save(sale);
            
            // Add sale items
            if (!products.isEmpty()) {
                SaleItem item1 = new SaleItem();
                item1.setSale(sale);
                item1.setProduct(products.get(0));
                item1.setQuantity(1);
                item1.setUnitPrice(products.get(0).getSellingPrice());
                item1.setSubtotal(products.get(0).getSellingPrice());
                session.save(item1);
            }
            
            if (!services.isEmpty()) {
                SaleItem item2 = new SaleItem();
                item2.setSale(sale);
                item2.setService(services.get(0));
                item2.setQuantity(1);
                item2.setUnitPrice(services.get(0).getPrice());
                item2.setSubtotal(services.get(0).getPrice());
                session.save(item2);
            }
            
            tx.commit();
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
    
    private static void initializeAppointments(SessionFactory sessionFactory, List<Customer> customers, 
            List<Vehicle> vehicles, List<Technician> technicians) {
        Session session = sessionFactory.openSession();
        Transaction tx = session.beginTransaction();
        
        try {
            // Create some sample appointments
            LocalDate today = LocalDate.now();
            
            Appointment appt1 = new Appointment();
            appt1.setCustomer(customers.get(0));
            appt1.setVehicle(vehicles.get(0));
            appt1.setTechnician(technicians.get(0));
            appt1.setTitle("Tire Rotation");
            appt1.setDescription("Regular tire rotation service");
            appt1.setStartTime(LocalDateTime.of(today, LocalTime.of(9, 0)));
            appt1.setEndTime(LocalDateTime.of(today, LocalTime.of(10, 0)));
            appt1.setStatus(AppointmentStatus.SCHEDULED);
            session.save(appt1);
            
            Appointment appt2 = new Appointment();
            appt2.setCustomer(customers.get(1));
            appt2.setVehicle(vehicles.get(1));
            appt2.setTechnician(technicians.get(1));
            appt2.setTitle("Wheel Alignment");
            appt2.setDescription("Four-wheel alignment service");
            appt2.setStartTime(LocalDateTime.of(today, LocalTime.of(10, 30)));
            appt2.setEndTime(LocalDateTime.of(today, LocalTime.of(11, 30)));
            appt2.setStatus(AppointmentStatus.SCHEDULED);
            session.save(appt2);
            
            Appointment appt3 = new Appointment();
            appt3.setCustomer(customers.get(2));
            appt3.setVehicle(vehicles.get(2));
            appt3.setTechnician(technicians.get(0));
            appt3.setTitle("New Tire Installation");
            appt3.setDescription("Install and balance 4 new tires");
            appt3.setStartTime(LocalDateTime.of(today, LocalTime.of(13, 0)));
            appt3.setEndTime(LocalDateTime.of(today, LocalTime.of(15, 0)));
            appt3.setStatus(AppointmentStatus.SCHEDULED);
            session.save(appt3);
            
            tx.commit();
            
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        } finally {
            session.close();
        }
    }
} 