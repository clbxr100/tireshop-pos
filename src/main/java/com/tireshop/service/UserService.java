package com.tireshop.service;

import com.tireshop.model.User;
import com.tireshop.model.TimeEntry;
import com.tireshop.dao.UserDao;
import com.tireshop.dao.TimeEntryDao;
import com.tireshop.util.DatabaseManager; // Assuming DatabaseManager provides SessionFactory
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDate; // For date range filtering
import java.util.Map; // For roleTabPermissions
import java.util.Set;   // For roleTabPermissions
import java.util.HashSet;
import java.util.Arrays;  // For Arrays.asList
import com.tireshop.util.SettingsService; // Make sure SettingsService is imported

public class UserService {
    private final UserDao userDao;
    private final TimeEntryDao timeEntryDao;
    
    // activeTimeEntries can remain in-memory as it represents current volatile state.
    // Or, it could be queried from DB (e.g., entries with null clockOut for a user).
    // For simplicity with existing logic, keeping it in-memory for now.
    private Map<String, TimeEntry> activeTimeEntries;

    // In-memory storage for tab permissions per role
    private Map<String, Set<String>> roleTabPermissions;

    private SettingsService settingsService; // Added field for SettingsService

    // Define tab names consistently (could be an Enum or public static final in a shared constants class)
    public static final String TAB_INVENTORY = "Inventory";
    public static final String TAB_SALES = "Sales";
    public static final String TAB_CUSTOMERS = "Customers";
    public static final String TAB_SERVICES = "Services";
    public static final String TAB_APPOINTMENTS = "Appointments";
    public static final String TAB_ADMIN_SETTINGS = "Admin Settings";
    public static final String TAB_REPORTS = "Reports";
    // Note: Dashboard is usually always visible and not part of this permission set.

    public UserService() {
        this.userDao = new UserDao(DatabaseManager.getSessionFactory()); 
        this.timeEntryDao = new TimeEntryDao(DatabaseManager.getSessionFactory()); // Initialize TimeEntryDao
        this.activeTimeEntries = new HashMap<>();
        this.settingsService = new SettingsService(); // Instantiate SettingsService
        this.roleTabPermissions = new HashMap<>();
        initializeDefaultTabPermissions(); // Load defaults first
        loadCustomTabPermissions(); // Then load custom/persisted ones
        System.out.println("[UserService] Initialized. DAOs for User and TimeEntry are set up.");
        System.out.println("[UserService] roleTabPermissions after init and load: " + roleTabPermissions);
        // Load active time entries on startup
        loadActiveTimeEntries();
    }

    private void initializeDefaultTabPermissions() {
        // ADMIN: All tabs (This will be the ultimate fallback and also set if not customized)
        roleTabPermissions.put("ADMIN", new HashSet<>(Arrays.asList(
            TAB_INVENTORY, TAB_SALES, TAB_CUSTOMERS, TAB_SERVICES, 
            TAB_APPOINTMENTS, TAB_ADMIN_SETTINGS, TAB_REPORTS
        )));
        // MANAGER: Most tabs, not admin settings
        roleTabPermissions.put("MANAGER", new HashSet<>(Arrays.asList(
            TAB_INVENTORY, TAB_SALES, TAB_CUSTOMERS, TAB_SERVICES, 
            TAB_APPOINTMENTS, TAB_REPORTS
        )));
        // FRONT_DESK: Sales, Customers, Appointments
        roleTabPermissions.put("FRONT_DESK", new HashSet<>(Arrays.asList(
            TAB_SALES, TAB_CUSTOMERS, TAB_APPOINTMENTS
        )));
        // TECHNICIAN: Appointments, Services
        roleTabPermissions.put("TECHNICIAN", new HashSet<>(Arrays.asList(
            TAB_APPOINTMENTS, TAB_SERVICES 
        )));
        System.out.println("[UserService] Default tab permissions map initialized: " + roleTabPermissions);
    }

    private void loadCustomTabPermissions() {
        Map<String, Set<String>> persistedPermissions = settingsService.loadRoleTabPermissions();
        if (!persistedPermissions.isEmpty()) {
            System.out.println("[UserService] Loading custom tab permissions from settings: " + persistedPermissions);
            // Merge: Persisted permissions can override defaults for non-ADMIN roles.
            // ADMIN role permissions are not meant to be changed from full access via this mechanism.
            persistedPermissions.forEach((role, tabs) -> {
                if (!"ADMIN".equalsIgnoreCase(role)) { // Do not override ADMIN defaults from persisted settings
                    roleTabPermissions.put(role.toUpperCase(), tabs);
                }
            });
        }
        // Ensure ADMIN role always has all permissions as a final check, overriding any persisted state for ADMIN.
        roleTabPermissions.put("ADMIN", new HashSet<>(Arrays.asList(
            TAB_INVENTORY, TAB_SALES, TAB_CUSTOMERS, TAB_SERVICES, 
            TAB_APPOINTMENTS, TAB_ADMIN_SETTINGS, TAB_REPORTS
        )));
        System.out.println("[UserService] Tab permissions after loading custom: " + roleTabPermissions);
    }

    public Set<String> getTabPermissionsForRole(String roleName) {
        if (roleName == null) return new HashSet<>();
        // Always ensure ADMIN has full access, regardless of what might be in the map due to loading order.
        if ("ADMIN".equalsIgnoreCase(roleName)) {
             return new HashSet<>(Arrays.asList(TAB_INVENTORY, TAB_SALES, TAB_CUSTOMERS, TAB_SERVICES, TAB_APPOINTMENTS, TAB_ADMIN_SETTINGS, TAB_REPORTS));
        }
        return roleTabPermissions.getOrDefault(roleName.toUpperCase(), new HashSet<>());
    }

    public void setTabPermissionsForRole(String roleName, Set<String> tabs) {
        if (roleName == null || tabs == null) return;
        String upperRoleName = roleName.toUpperCase();

        if ("ADMIN".equalsIgnoreCase(upperRoleName)) {
            System.out.println("[UserService] Attempt to modify ADMIN role tab permissions ignored. ADMIN always has full access.");
            // Ensure ADMIN defaults are in the map if somehow it got removed, then save.
            roleTabPermissions.put("ADMIN", new HashSet<>(Arrays.asList(TAB_INVENTORY, TAB_SALES, TAB_CUSTOMERS, TAB_SERVICES, TAB_APPOINTMENTS, TAB_ADMIN_SETTINGS, TAB_REPORTS)));
            settingsService.saveRoleTabPermissions(roleTabPermissions);
            settingsService.saveProperties(); // Persist to file
            return;
        }
        roleTabPermissions.put(upperRoleName, new HashSet<>(tabs)); 
        System.out.println("[UserService] Updated tab permissions for role '" + upperRoleName + "': " + tabs);
        settingsService.saveRoleTabPermissions(roleTabPermissions); // Save the whole map
        settingsService.saveProperties(); // Persist to file
        System.out.println("[UserService] All role tab permissions saved to settings.");
    }

    private void loadActiveTimeEntries() {
        List<TimeEntry> allEntries = timeEntryDao.findAll(); // Consider a more targeted query if performance is an issue
        for (TimeEntry entry : allEntries) {
            if (entry.getClockOut() == null) {
                activeTimeEntries.put(entry.getUserId(), entry);
                System.out.println("[UserService] Loaded active clock-in for user ID: " + entry.getUserId() + " from DB.");
            }
        }
    }

    // User Management - Now uses UserDao
    public User addUser(User user) {
        if (user.getId() == null) {
            user.setId(UUID.randomUUID().toString());
        }
        user.setRole(user.getRole().toUpperCase()); // Ensure role is uppercase
        
        // Check if username already exists
        if (userDao.findByUsername(user.getUsername()).isPresent()) {
            System.err.println("[UserService] Add user failed: Username '" + user.getUsername() + "' already exists.");
            // Optionally throw an exception or return null/error indicator
            return null; 
        }
        try {
            User savedUser = userDao.saveOrUpdateUser(user);
            System.out.println("[UserService] Added user to DB: " + savedUser.getUsername() + " with role: " + savedUser.getRole());
            return savedUser;
        } catch (Exception e) {
            System.err.println("[UserService] Error adding user '" + user.getUsername() + "' to DB: " + e.getMessage());
            return null;
        }
    }

    public User getUser(String id) {
        Optional<User> userOpt = userDao.findById(id);
        return userOpt.orElse(null);
    }
    
    public User getUserByUsername(String username) {
        Optional<User> userOpt = userDao.findByUsername(username);
        return userOpt.orElse(null);
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    public User updateUser(User user) {
        user.setRole(user.getRole().toUpperCase());
        // Ensure user exists before attempting update by ID
        if (userDao.findById(user.getId()).isPresent()) {
             // Prevent changing username if it's part of unique constraint or business logic
            User existingUserByUsername = userDao.findByUsername(user.getUsername()).orElse(null);
            if (existingUserByUsername != null && !existingUserByUsername.getId().equals(user.getId())) {
                System.err.println("[UserService] Update user failed: Username '" + user.getUsername() + "' is already taken by another user.");
                return null;
            }
            try {
                User updatedUser = userDao.saveOrUpdateUser(user);
                System.out.println("[UserService] Updated user in DB: " + updatedUser.getUsername());
                return updatedUser;
            } catch (Exception e) {
                 System.err.println("[UserService] Error updating user '" + user.getUsername() + "' in DB: " + e.getMessage());
                return null;
            }
        } else {
            System.err.println("[UserService] Update failed: User with ID '" + user.getId() + "' not found.");
            return null;
        }
    }

    public void deleteUser(String id) {
        if (userDao.findById(id).isPresent()) {
            userDao.deleteById(id);
            System.out.println("[UserService] Deleted user with ID: " + id + " from DB.");
        } else {
            System.err.println("[UserService] Delete failed: User with ID '" + id + "' not found.");
        }
    }

    public User authenticateUser(String username, String password) {
        System.out.println("[UserService] Authenticating user from DB: " + username);
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.isActive() && user.getPassword().equals(password)) { // Simple password check
                System.out.println("[UserService] DB Authentication successful for user: " + username + " with role: " + user.getRole());
                return user;
            }
        }
        System.out.println("[UserService] DB Authentication failed for user: " + username);
        return null;
    }

    // Time Entry Management - Now uses TimeEntryDao
    public TimeEntry clockIn(String userId, String notes) {
        System.out.println("[UserService] Clock-in attempt for user ID: " + userId);
        if (isUserClockedIn(userId)) { // isUserClockedIn now checks activeTimeEntries map which is loaded from DB
            System.out.println("[UserService] User ID: " + userId + " is already clocked in (checked in-memory active map). Clock-in failed.");
            return null;
        }
        User user = userDao.findById(userId).orElse(null); // Fetch from DB
        if (user == null) {
             System.out.println("[UserService] User ID: " + userId + " not found in DB. Clock-in failed.");
            return null;
        }

        TimeEntry entry = new TimeEntry(
            UUID.randomUUID().toString(),
            userId,
            LocalDateTime.now(),
            null,
            notes
        );
        try {
            timeEntryDao.saveTimeEntry(entry); // Changed from saveOrUpdateTimeEntry to saveTimeEntry
            activeTimeEntries.put(userId, entry); // Also update in-memory active map
            System.out.println("[UserService] User ID: " + userId + " clocked in. Entry saved to DB. ID: " + entry.getId());
            return entry;
        } catch (Exception e) {
            System.err.println("[UserService] Error saving clock-in entry to DB for user ID: " + userId + ": " + e.getMessage());
            return null;
        }
    }

    public TimeEntry clockOut(String userId, String notes) {
        System.out.println("[UserService] Clock-out attempt for user ID: " + userId);
        TimeEntry entryToModify = activeTimeEntries.get(userId); // Get from in-memory active map
        boolean entryFromActiveMap = true;

        if (entryToModify == null) {
            entryFromActiveMap = false;
            System.out.println("[UserService] User ID: " + userId + " not found in active clock-ins map. Trying DB for an open entry.");
            List<TimeEntry> userEntries = timeEntryDao.findByUserId(userId);
            entryToModify = userEntries.stream().filter(e -> e.getClockOut() == null).findFirst().orElse(null);
            if(entryToModify == null){
                System.out.println("[UserService] User ID: " + userId + " has no active clock-in in DB. Clock-out failed.");
                return null;
            }
             System.out.println("[UserService] Found active clock-in for user ID: " + userId + " in DB. Proceeding with clock-out.");
        }

        LocalDateTime originalClockOutTime = entryToModify.getClockOut();

        entryToModify.setClockOut(LocalDateTime.now());
        String existingNotes = entryToModify.getNotes() == null ? "" : entryToModify.getNotes();
        String newNotes = notes == null ? "" : notes;
        if (!newNotes.isEmpty()) {
            entryToModify.setNotes(existingNotes.isEmpty() ? newNotes : existingNotes + " | " + newNotes);
        }
        
        try {
            timeEntryDao.updateTimeEntry(entryToModify); // Changed from saveOrUpdateTimeEntry to updateTimeEntry
            if (entryFromActiveMap) { 
                activeTimeEntries.remove(userId); 
            }
            System.out.println("[UserService] User ID: " + userId + " clocked out. Entry updated in DB. ID: " + entryToModify.getId());
            return entryToModify;
        } catch (Exception e) {
            System.err.println("[UserService] Error updating clock-out entry in DB for user ID: " + userId + ": " + e.getMessage());
            if (entryFromActiveMap && entryToModify != null) {
                System.out.println("[UserService] Reverting clock-out time on in-memory entry due to DB save failure for user ID: " + userId);
                entryToModify.setClockOut(originalClockOutTime);
            }
            return null;
        }
    }

    // isUserClockedIn relies on the activeTimeEntries map, which is loaded on startup
    public boolean isUserClockedIn(String userId) {
        boolean clockedIn = activeTimeEntries.containsKey(userId);
        System.out.println("[UserService] Checking (in-memory) clock-in status for user ID: " + userId + ". Is clocked in: " + clockedIn);
        return clockedIn;
    }

    // getCurrentTimeEntry relies on the activeTimeEntries map
    public TimeEntry getCurrentTimeEntry(String userId) {
        TimeEntry entry = activeTimeEntries.get(userId);
        if (entry != null) {
            System.out.println("[UserService] Fetched current (in-memory) time entry for user ID: " + userId + ". Entry ID: " + entry.getId());
        } else {
            System.out.println("[UserService] No current (in-memory active) time entry found for user ID: " + userId);
        }
        return entry;
    }

    // Fetching historical entries now uses DAO
    public List<TimeEntry> getUserTimeEntries(String userId) {
        System.out.println("[UserService] Fetching all time entries from DB for user ID: " + userId);
        return timeEntryDao.findByUserId(userId);
    }

    public List<TimeEntry> getAllTimeEntries() {
        System.out.println("[UserService] Fetching all time entries from DB.");
        return timeEntryDao.findAll();
    }

    public List<TimeEntry> getTimeEntriesByDateRange(LocalDate startDate, LocalDate endDate) {
        System.out.println("[UserService] Fetching time entries from DB for date range: " + startDate + " to " + endDate);
        return timeEntryDao.findByDateRange(startDate, endDate);
    }

    public List<TimeEntry> getTimeEntriesByUserIdAndDateRange(String userId, LocalDate startDate, LocalDate endDate) {
        System.out.println("[UserService] Fetching time entries from DB for user ID: " + userId + " and date range: " + startDate + " to " + endDate);
        return timeEntryDao.findByUserIdAndDateRange(userId, startDate, endDate);
    }

    // Helper methods for reporting
    public double calculateTotalHours(String userId, LocalDateTime start, LocalDateTime end) {
        return timeEntryDao.findByUserIdAndDateRange(userId, start.toLocalDate(), end.toLocalDate()).stream()
            .filter(e -> e.getClockOut() != null && // Ensure there is a clock-out time
                        !e.getClockIn().isBefore(start) && 
                        !e.getClockOut().isAfter(end))
            .mapToDouble(TimeEntry::getDurationHours)
            .sum();
    }

    // Method for Admin to add a manual time entry
    public TimeEntry addManualTimeEntry(String userId, LocalDateTime clockInTime, LocalDateTime clockOutTime, String notes) {
        System.out.println("[UserService] Attempting to add manual time entry for user ID: " + userId);
        User user = userDao.findById(userId).orElse(null);
        if (user == null) {
            System.err.println("[UserService] User ID: " + userId + " not found in DB. Manual time entry failed.");
            return null; // Or throw IllegalArgumentException
        }

        if (clockInTime == null || clockOutTime == null) {
            System.err.println("[UserService] Clock-in and Clock-out times must be provided for manual entry.");
            return null; // Or throw IllegalArgumentException
        }

        if (!clockInTime.isBefore(clockOutTime)) {
            System.err.println("[UserService] Clock-in time must be before clock-out time for manual entry.");
            return null; // Or throw IllegalArgumentException
        }

        // Check for overlapping entries for the same user - might be complex and optional based on requirements
        // For simplicity, this example does not check for overlaps with existing entries.

        TimeEntry entry = new TimeEntry(
            UUID.randomUUID().toString(),
            userId,
            clockInTime,
            clockOutTime,
            notes
        );

        try {
            TimeEntry savedEntry = timeEntryDao.saveTimeEntry(entry);
            System.out.println("[UserService] Manual time entry saved to DB for user ID: " + userId + ". Entry ID: " + savedEntry.getId());
            // Note: This manual entry does not affect the activeTimeEntries map, as it's for historical/corrective purposes.
            return savedEntry;
        } catch (Exception e) {
            System.err.println("[UserService] Error saving manual time entry to DB for user ID: " + userId + ": " + e.getMessage());
            return null;
        }
    }

    // Method for Admin to update a time entry
    public TimeEntry updateTimeEntry(TimeEntry timeEntry) {
        System.out.println("[UserService] Updating time entry ID: " + timeEntry.getId());
        try {
            TimeEntry updatedEntry = timeEntryDao.updateTimeEntry(timeEntry);
            
            // Update activeTimeEntries if this is an active entry
            if (timeEntry.getClockOut() == null) {
                activeTimeEntries.put(timeEntry.getUserId(), updatedEntry);
            } else {
                // Remove from active if it was completed
                activeTimeEntries.remove(timeEntry.getUserId());
            }
            
            System.out.println("[UserService] Time entry updated successfully: " + timeEntry.getId());
            return updatedEntry;
        } catch (Exception e) {
            System.err.println("[UserService] Error updating time entry ID: " + timeEntry.getId() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Method for Admin to delete a time entry
    public boolean deleteTimeEntry(String timeEntryId) {
        System.out.println("[UserService] Deleting time entry ID: " + timeEntryId);
        try {
            timeEntryDao.deleteById(timeEntryId);
            
            // Also remove from active entries if it was active
            activeTimeEntries.values().removeIf(entry -> entry.getId().equals(timeEntryId));
            
            System.out.println("[UserService] Time entry deleted successfully: " + timeEntryId);
            return true;
        } catch (Exception e) {
            System.err.println("[UserService] Error deleting time entry ID: " + timeEntryId + " - " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
} 