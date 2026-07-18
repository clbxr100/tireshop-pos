package com.tireshop.util;

import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.geometry.Rectangle2D;

import java.util.prefs.Preferences;

/**
 * Manages theme switching and responsive scaling for the application
 */
public class ThemeManager {
    
    public enum Theme {
        LIGHT("Light"),
        DARK("Dark");
        
        private final String displayName;
        
        Theme(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }
    
    private static final String THEME_PREF_KEY = "app_theme";
    private static final Preferences preferences = Preferences.userNodeForPackage(ThemeManager.class);
    
    // Screen size breakpoints
    private static final double SMALL_SCREEN_MAX = 1366;
    private static final double MEDIUM_SCREEN_MAX = 1599;
    private static final double LARGE_SCREEN_MAX = 1919;
    private static final double EXTRA_LARGE_SCREEN_MAX = 2559;
    // Ultra-wide is anything above extra-large
    
    // DPI thresholds
    private static final double HIGH_DPI_THRESHOLD = 144; // Standard high DPI
    private static final double ULTRA_HIGH_DPI_THRESHOLD = 192; // Ultra high DPI
    
    /**
     * Apply theme to a scene with responsive scaling
     */
    public static void applyTheme(Scene scene, Theme theme) {
        // Remove all theme-related stylesheets first
        scene.getStylesheets().clear();
        
        // Remove theme-specific root classes
        scene.getRoot().getStyleClass().removeAll("dark-theme", "light-theme");
        
        if (theme == Theme.DARK) {
            // For dark theme, load ONLY the dark theme CSS (it contains all necessary styles)
            scene.getStylesheets().add(ThemeManager.class.getResource("/styles/dark-theme.css").toExternalForm());
            scene.getRoot().getStyleClass().add("dark-theme");
        } else {
            // For light theme, load ONLY the modern CSS
            scene.getStylesheets().add(ThemeManager.class.getResource("/styles/modern.css").toExternalForm());
            scene.getRoot().getStyleClass().add("light-theme");
        }
        
        // Always load table fixes CSS last to override any conflicting styles
        try {
            scene.getStylesheets().add(ThemeManager.class.getResource("/styles/table-fixes.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("[ThemeManager] Warning: Could not load table-fixes.css: " + e.getMessage());
        }
        
        // Load responsive fixes CSS to disable problematic scaling
        try {
            scene.getStylesheets().add(ThemeManager.class.getResource("/styles/responsive-fixes.css").toExternalForm());
        } catch (Exception e) {
            System.err.println("[ThemeManager] Warning: Could not load responsive-fixes.css: " + e.getMessage());
        }
        
        // Apply responsive scaling
        applyResponsiveScaling(scene);
    }
    
    /**
     * Apply responsive scaling based on screen size and DPI
     */
    public static void applyResponsiveScaling(Scene scene) {
        if (scene == null) return;
        
        Platform.runLater(() -> {
            try {
                // Get screen information
                Rectangle2D screenBounds = Screen.getPrimary().getBounds();
                double screenWidth = screenBounds.getWidth();
                
                // Remove ALL existing responsive classes to prevent conflicts
                scene.getRoot().getStyleClass().removeAll(
                    "small-screen", "medium-screen", "large-screen", 
                    "extra-large-screen", "ultra-wide-screen", 
                    "high-dpi", "ultra-high-dpi"
                );
                
                // Apply ONLY ONE simple size class to prevent CSS conflicts
                // This is much simpler and reduces the complexity that was causing text issues
                if (screenWidth <= 1366) {
                    scene.getRoot().getStyleClass().add("small-screen");
                } else if (screenWidth >= 1920) {
                    scene.getRoot().getStyleClass().add("large-screen");
                }
                // For medium screens (1366-1919), we don't add any class (use defaults)
                
                System.out.println("[ThemeManager] Applied simplified responsive scaling for width: " + (int)screenWidth);
                
            } catch (Exception e) {
                System.err.println("[ThemeManager] Error applying responsive scaling: " + e.getMessage());
                // No fallback class - let it use default styles
            }
        });
    }
    
    /**
     * Determine screen size class based on width
     */
    private static String determineScreenSizeClass(double screenWidth) {
        if (screenWidth <= SMALL_SCREEN_MAX) {
            return "small-screen";
        } else if (screenWidth <= MEDIUM_SCREEN_MAX) {
            return "medium-screen";
        } else if (screenWidth <= LARGE_SCREEN_MAX) {
            return "large-screen";
        } else if (screenWidth <= EXTRA_LARGE_SCREEN_MAX) {
            return "extra-large-screen";
        } else {
            return "ultra-wide-screen";
        }
    }
    
    /**
     * Determine DPI class if high DPI scaling is needed
     */
    private static String determineDpiClass(double dpi) {
        if (dpi >= ULTRA_HIGH_DPI_THRESHOLD) {
            return "ultra-high-dpi";
        } else if (dpi >= HIGH_DPI_THRESHOLD) {
            return "high-dpi";
        }
        return null; // Standard DPI, no class needed
    }
    
    /**
     * Set up dynamic responsive scaling that updates when window is resized
     */
    public static void setupDynamicScaling(Stage stage, Scene scene) {
        if (stage == null || scene == null) return;
        
        // Listen for stage size changes
        ChangeListener<Number> sizeChangeListener = (obs, oldVal, newVal) -> {
            Platform.runLater(() -> applyResponsiveScaling(scene));
        };
        
        stage.widthProperty().addListener(sizeChangeListener);
        stage.heightProperty().addListener(sizeChangeListener);
        
        // Listen for stage moving between screens (different DPI)
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> applyResponsiveScaling(scene));
        });
        
        stage.yProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> applyResponsiveScaling(scene));
        });
        
        // Apply initial scaling
        applyResponsiveScaling(scene);
    }
    
    /**
     * Get the current theme preference
     */
    public static Theme getCurrentTheme() {
        String themeName = preferences.get(THEME_PREF_KEY, Theme.LIGHT.name());
        try {
            return Theme.valueOf(themeName);
        } catch (IllegalArgumentException e) {
            return Theme.LIGHT;
        }
    }
    
    /**
     * Toggle between light and dark themes
     */
    public static Theme toggleTheme(Scene scene) {
        Theme currentTheme = getCurrentTheme();
        Theme newTheme = (currentTheme == Theme.LIGHT) ? Theme.DARK : Theme.LIGHT;
        applyTheme(scene, newTheme);
        saveTheme(newTheme);
        return newTheme;
    }
    
    /**
     * Apply saved theme preference to a scene
     */
    public static void applySavedTheme(Scene scene) {
        applyTheme(scene, getCurrentTheme());
    }
    
    /**
     * Get CSS class for current screen size (for debugging/info)
     */
    public static String getCurrentScreenSizeInfo() {
        try {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            double screenWidth = screenBounds.getWidth();
            double screenHeight = screenBounds.getHeight();
            double dpi = Screen.getPrimary().getDpi();
            
            String sizeClass = determineScreenSizeClass(screenWidth);
            String dpiClass = determineDpiClass(dpi);
            
            return String.format("Screen: %.0fx%.0f, DPI: %.0f, Class: %s%s", 
                screenWidth, screenHeight, dpi, sizeClass, 
                (dpiClass != null ? " + " + dpiClass : ""));
        } catch (Exception e) {
            return "Screen info unavailable";
        }
    }
    
    /**
     * Force refresh of responsive scaling (useful after display changes)
     */
    public static void refreshResponsiveScaling(Scene scene) {
        if (scene != null) {
            Platform.runLater(() -> {
                // Re-apply current theme which includes responsive scaling
                Theme currentTheme = getCurrentTheme();
                applyTheme(scene, currentTheme);
            });
        }
    }
    
    /**
     * Check if current display is high DPI
     */
    public static boolean isHighDpi() {
        try {
            double dpi = Screen.getPrimary().getDpi();
            return dpi >= HIGH_DPI_THRESHOLD;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get recommended font scale factor for current display
     */
    public static double getRecommendedFontScale() {
        try {
            Rectangle2D screenBounds = Screen.getPrimary().getBounds();
            double screenWidth = screenBounds.getWidth();
            double dpi = Screen.getPrimary().getDpi();
            
            // Base scale factor
            double scale = 1.0;
            
            // Adjust for screen size
            if (screenWidth <= SMALL_SCREEN_MAX) {
                scale = 0.85;
            } else if (screenWidth <= MEDIUM_SCREEN_MAX) {
                scale = 0.95;
            } else if (screenWidth <= LARGE_SCREEN_MAX) {
                scale = 1.1;
            } else if (screenWidth <= EXTRA_LARGE_SCREEN_MAX) {
                scale = 1.2;
            } else {
                scale = 1.35;
            }
            
            // Adjust for DPI
            if (dpi >= ULTRA_HIGH_DPI_THRESHOLD) {
                scale *= 1.15;
            } else if (dpi >= HIGH_DPI_THRESHOLD) {
                scale *= 1.05;
            }
            
            return scale;
        } catch (Exception e) {
            return 1.0; // Default scale
        }
    }
    
    public static void saveTheme(Theme theme) {
        preferences.put(THEME_PREF_KEY, theme.name());
    }
} 