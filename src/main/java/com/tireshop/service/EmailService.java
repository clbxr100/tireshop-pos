package com.tireshop.service;

import com.tireshop.model.Appointment;
import com.tireshop.model.Customer;
import com.tireshop.model.Sale;
import com.tireshop.util.SettingsService;
import org.hibernate.SessionFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Service for sending email notifications
 */
public class EmailService {
    
    private static final Logger LOGGER = Logger.getLogger(EmailService.class.getName());

    // Settings keys (stored in config.properties)
    public static final String KEY_SMTP_HOST = "email.smtp.host";
    public static final String KEY_SMTP_PORT = "email.smtp.port";
    public static final String KEY_SMTP_USERNAME = "email.username";
    public static final String KEY_SMTP_PASSWORD = "email.password";
    public static final String KEY_FROM = "email.from";

    private final SettingsService settingsService;
    private Session mailSession;
    
    public EmailService(SettingsService settingsService) {
        this.settingsService = settingsService;
        initializeMailSession();
    }
    
    private void initializeMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", settingsService.getSetting(KEY_SMTP_HOST, "smtp.gmail.com"));
        props.put("mail.smtp.port", settingsService.getSetting(KEY_SMTP_PORT, "587"));

        String username = settingsService.getSetting(KEY_SMTP_USERNAME, "");
        String password = settingsService.getSetting(KEY_SMTP_PASSWORD, "");
        
        mailSession = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
    }
    
    /**
     * Send appointment reminder email
     */
    public boolean sendAppointmentReminder(Appointment appointment) {
        if (appointment.getCustomer() == null || appointment.getCustomer().getEmail() == null) {
            return false;
        }
        
        String subject = "Appointment Reminder - " + settingsService.getSetting("shop.name", "Tire Shop");
        String body = buildAppointmentReminderBody(appointment);
        
        return sendEmail(appointment.getCustomer().getEmail(), subject, body);
    }
    
    /**
     * Send appointment confirmation email
     */
    public boolean sendAppointmentConfirmation(Appointment appointment) {
        if (appointment.getCustomer() == null || appointment.getCustomer().getEmail() == null) {
            return false;
        }
        
        String subject = "Appointment Confirmed - " + settingsService.getSetting("shop.name", "Tire Shop");
        String body = buildAppointmentConfirmationBody(appointment);
        
        return sendEmail(appointment.getCustomer().getEmail(), subject, body);
    }
    
    /**
     * Send service completion notification
     */
    public boolean sendServiceCompletionNotification(Sale sale) {
        if (sale.getCustomer() == null || sale.getCustomer().getEmail() == null) {
            return false;
        }
        
        String subject = "Service Completed - " + settingsService.getSetting("shop.name", "Tire Shop");
        String body = buildServiceCompletionBody(sale);
        
        return sendEmail(sale.getCustomer().getEmail(), subject, body);
    }
    
    /**
     * Send low stock alert to manager
     */
    public boolean sendLowStockAlert(String productName, int currentStock, int reorderLevel) {
        String managerEmail = settingsService.getSetting("manager.email", "");
        if (managerEmail.isEmpty()) {
            return false;
        }
        
        String subject = "Low Stock Alert - " + productName;
        String body = String.format(
            "The following product is running low on stock:\n\n" +
            "Product: %s\n" +
            "Current Stock: %d\n" +
            "Reorder Level: %d\n\n" +
            "Please consider placing a reorder soon.",
            productName, currentStock, reorderLevel
        );
        
        return sendEmail(managerEmail, subject, body);
    }
    
    /**
     * Send a plain-text email to any address (e.g. daily reports to the owner).
     * Re-reads the SMTP settings each time so changes apply without a restart.
     */
    public boolean sendReportEmail(String to, String subject, String body) {
        if (to == null || to.trim().isEmpty()) {
            LOGGER.warning("Cannot send report - no recipient email configured");
            return false;
        }
        String username = settingsService.getSetting("email.username", "");
        if (username.isEmpty()) {
            LOGGER.warning("Cannot send report - email.username is not configured in Admin settings");
            return false;
        }
        initializeMailSession(); // pick up the latest SMTP settings
        return sendEmail(to, subject, body);
    }

    private boolean sendEmail(String to, String subject, String body) {
        try {
            Message message = new MimeMessage(mailSession);
            message.setFrom(new InternetAddress(settingsService.getSetting(KEY_FROM, "noreply@tireshop.com")));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            
            Transport.send(message);
            LOGGER.info("Email sent successfully to: " + to);
            return true;
            
        } catch (MessagingException e) {
            LOGGER.severe("Failed to send email: " + e.getMessage());
            return false;
        }
    }
    
    private String buildAppointmentReminderBody(Appointment appointment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
        return String.format(
            "Dear %s,\n\n" +
            "This is a reminder about your upcoming appointment:\n\n" +
            "Date & Time: %s\n" +
            "Service: %s\n" +
            "Vehicle: %s\n\n" +
            "Location: %s\n" +
            "%s\n\n" +
            "If you need to reschedule, please call us at %s.\n\n" +
            "Thank you,\n%s",
            appointment.getCustomer().getFirstName(),
            appointment.getStartTime().format(formatter),
            appointment.getTitle(),
            appointment.getVehicle() != null ? 
                appointment.getVehicle().getModelYear() + " " + 
                appointment.getVehicle().getMake() + " " + 
                appointment.getVehicle().getModel() : "N/A",
            settingsService.getSetting("shop.address", ""),
            settingsService.getSetting("shop.city", "") + ", " + 
                settingsService.getSetting("shop.state", "") + " " + 
                settingsService.getSetting("shop.zip", ""),
            settingsService.getSetting("shop.phone", ""),
            settingsService.getSetting("shop.name", "Tire Shop")
        );
    }
    
    private String buildAppointmentConfirmationBody(Appointment appointment) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy 'at' h:mm a");
        
        // Calculate duration in minutes
        long durationMinutes = 0;
        if (appointment.getStartTime() != null && appointment.getEndTime() != null) {
            durationMinutes = java.time.Duration.between(
                appointment.getStartTime(), 
                appointment.getEndTime()
            ).toMinutes();
        }
        
        return String.format(
            "Dear %s,\n\n" +
            "Your appointment has been confirmed!\n\n" +
            "Date & Time: %s\n" +
            "Service: %s\n" +
            "Estimated Duration: %d minutes\n" +
            "Vehicle: %s\n\n" +
            "Please arrive 10 minutes early to complete any necessary paperwork.\n\n" +
            "Location: %s\n" +
            "%s\n\n" +
            "We look forward to seeing you!\n\n" +
            "Thank you,\n%s",
            appointment.getCustomer().getFirstName(),
            appointment.getStartTime().format(formatter),
            appointment.getTitle(),
            durationMinutes,
            appointment.getVehicle() != null ? 
                appointment.getVehicle().getModelYear() + " " + 
                appointment.getVehicle().getMake() + " " + 
                appointment.getVehicle().getModel() : "N/A",
            settingsService.getSetting("shop.address", ""),
            settingsService.getSetting("shop.city", "") + ", " + 
                settingsService.getSetting("shop.state", "") + " " + 
                settingsService.getSetting("shop.zip", ""),
            settingsService.getSetting("shop.phone", ""),
            settingsService.getSetting("shop.name", "Tire Shop")
        );
    }
    
    private String buildServiceCompletionBody(Sale sale) {
        return String.format(
            "Dear %s,\n\n" +
            "Your vehicle service has been completed!\n\n" +
            "Invoice #: %s\n" +
            "Total: $%.2f\n\n" +
            "Thank you for choosing %s. We appreciate your business!\n\n" +
            "If you have any questions about the service performed, please don't hesitate to contact us.\n\n" +
            "Best regards,\n%s\n%s",
            sale.getCustomer().getFirstName(),
            sale.getInvoiceNumber(),
            sale.getTotal(),
            settingsService.getSetting("shop.name", "Tire Shop"),
            settingsService.getSetting("shop.name", "Tire Shop"),
            settingsService.getSetting("shop.phone", "")
        );
    }
} 