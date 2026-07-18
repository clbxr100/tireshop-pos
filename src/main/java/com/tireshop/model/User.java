package com.tireshop.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Column;
// No need forGeneratedValue if ID is username or a manually assigned UUID string

@Entity
@Table(name = "users") // Specifies the database table name
public class User {

    @Id
    @Column(name = "id", unique = true, nullable = false, length = 36)
    private String id; // Can be username or a UUID string

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", nullable = false)
    private String password; // In a real app, this should be hashed

    @Column(name = "role", nullable = false)
    private String role; // ADMIN, MANAGER, FRONT_DESK, TECHNICIAN

    @Column(name = "active", nullable = false)
    private boolean active;

    public User() {
    }

    // Constructor for creating new users (ID can be generated or username)
    public User(String id, String username, String password, String role, boolean active) {
        this.id = id; // Ensure ID is set. If using username as ID, pass username here too.
        this.username = username;
        this.password = password;
        this.role = role.toUpperCase(); // Store role in uppercase
        this.active = active;
    }

    // Getters and Setters (ensure all are present)
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role.toUpperCase(); // Ensure role is always stored in uppercase
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "User{" +
               "id='" + id + '\'' +
               ", username='" + username + '\'' +
               ", role='" + role + '\'' +
               ", active=" + active +
               '}';
    }
} 