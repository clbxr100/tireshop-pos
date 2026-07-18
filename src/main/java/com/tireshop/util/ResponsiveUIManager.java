package com.tireshop.util;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.geometry.Rectangle2D;

/**
 * Enhanced utility class to manage responsive UI behavior.
 * Automatically adapts to system DPI scaling and screen sizes for optimal display.
 */
public class ResponsiveUIManager {
    
    // Base screen size breakpoints (before DPI scaling)
    private static final double SMALL_SCREEN_WIDTH = 1024;
    private static final double LARGE_SCREEN_WIDTH = 1440;
    private static final double EXTRA_LARGE_SCREEN_WIDTH = 1920;
    
    // CSS class names
    private static final String SMALL_SCREEN_CLASS = "small-screen";
    private static final String LARGE_SCREEN_CLASS = "large-screen";
    private static final String EXTRA_LARGE_SCREEN_CLASS = "extra-large-screen";
    private static final String HIGH_DPI_CLASS = "high-dpi";
    
    // System scaling detection
    private static double detectedSystemScale = -1;
    private static boolean systemScaleDetected = false;
    
    /**
     * Initialize responsive behavior for a stage with enhanced DPI detection
     * @param stage The primary stage to make responsive
     */
    public static void initializeResponsiveUI(Stage stage) {
        if (stage == null || stage.getScene() == null) {
            System.err.println("ResponsiveUIManager: Stage or Scene is null, cannot initialize responsive UI");
            return;
        }
        
        Scene scene = stage.getScene();
        
        // Detect system DPI scaling on first run
        detectSystemDPIScaling(stage);
        
        // Apply initial responsive class based on current screen
        updateResponsiveClass(scene, stage.getWidth());
        
        // Listen for window width changes
        stage.widthProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
                Platform.runLater(() -> {
                    updateResponsiveClass(scene, newValue.doubleValue());
                });
            }
        });
        
        // Listen for maximized state changes
        stage.maximizedProperty().addListener(new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
                Platform.runLater(() -> {
                    double width = newValue ? Screen.getPrimary().getVisualBounds().getWidth() : stage.getWidth();
                    updateResponsiveClass(scene, width);
                });
            }
        });
        
        // Listen for DPI changes (monitor switching, etc.)
        stage.xProperty().addListener((obs, oldVal, newVal) -> {
            Platform.runLater(() -> {
                detectSystemDPIScaling(stage);
                updateResponsiveClass(scene, stage.getWidth());
            });
        });
        
        System.out.println("ResponsiveUIManager: Initialized responsive UI for stage with DPI scaling support");
        System.out.println("ResponsiveUIManager: Detected system scale factor: " + getSystemScaleFactor());
    }
    
    /**
     * Detect system DPI scaling factor
     * @param stage The stage to use for detection
     */
    private static void detectSystemDPIScaling(Stage stage) {
        if (systemScaleDetected) return; // Only detect once unless explicitly refreshed
        
        try {
            // Get the screen containing the stage
            Screen currentScreen = Screen.getPrimary();
            for (Screen screen : Screen.getScreens()) {
                Rectangle2D bounds = screen.getBounds();
                if (bounds.contains(stage.getX() + stage.getWidth()/2, stage.getY() + stage.getHeight()/2)) {
                    currentScreen = screen;
                    break;
                }
            }
            
            // Calculate DPI scale factor
            Rectangle2D bounds = currentScreen.getBounds();
            Rectangle2D visualBounds = currentScreen.getVisualBounds();
            
            // JavaFX reports scaled coordinates, so we compare with system properties
            double javaFXWidth = bounds.getWidth();
            
            // Try to get actual pixel dimensions from system properties
            String screenResolution = System.getProperty("java.awt.headless");
            
            // Alternative method: Use output scale directly
            double outputScaleX = currentScreen.getOutputScaleX();
            double outputScaleY = currentScreen.getOutputScaleY();
            
            // Use the larger of the two scales
            detectedSystemScale = Math.max(outputScaleX, outputScaleY);
            
            // Fallback: if scale is 1.0 but we suspect scaling, use logical detection
            if (detectedSystemScale == 1.0) {
                // If window appears smaller than expected for resolution, scaling is likely active
                if (javaFXWidth < 1920 && System.getProperty("os.name").toLowerCase().contains("windows")) {
                    // Common Windows scaling scenarios
                    if (javaFXWidth <= 1280) {
                        detectedSystemScale = 1.5; // 150% scaling
                    } else if (javaFXWidth <= 1600) {
                        detectedSystemScale = 1.25; // 125% scaling
                    }
                }
            }
            
            systemScaleDetected = true;
            System.out.println("ResponsiveUIManager: Detected DPI scale factor: " + detectedSystemScale + 
                             " (OutputScale: " + outputScaleX + "x" + outputScaleY + ")");
            
        } catch (Exception e) {
            System.err.println("ResponsiveUIManager: Error detecting DPI scaling: " + e.getMessage());
            detectedSystemScale = 1.0; // Default to no scaling
            systemScaleDetected = true;
        }
    }
    
    /**
     * Update the responsive CSS class based on current width and DPI
     * @param scene The scene to update
     * @param width Current window width
     */
    private static void updateResponsiveClass(Scene scene, double width) {
        if (scene == null || scene.getRoot() == null) {
            return;
        }
        
        // Remove all responsive classes first
        scene.getRoot().getStyleClass().removeAll(
            SMALL_SCREEN_CLASS, 
            LARGE_SCREEN_CLASS, 
            EXTRA_LARGE_SCREEN_CLASS,
            HIGH_DPI_CLASS
        );
        
        // Adjust width based on system DPI scaling
        double effectiveWidth = width * getSystemScaleFactor();
        
        // Add high DPI class if system scaling is detected
        if (getSystemScaleFactor() > 1.1) {
            scene.getRoot().getStyleClass().add(HIGH_DPI_CLASS);
        }
        
        // Add size-based class
        String newClass = getResponsiveClass(effectiveWidth);
        if (newClass != null) {
            scene.getRoot().getStyleClass().add(newClass);
            System.out.println("ResponsiveUIManager: Applied class '" + newClass + "' for effective width " + 
                             effectiveWidth + " (actual: " + width + ", scale: " + getSystemScaleFactor() + ")");
        }
    }
    
    /**
     * Determine the appropriate responsive CSS class for the given width
     * @param effectiveWidth Window width adjusted for DPI scaling
     * @return CSS class name or null for default
     */
    private static String getResponsiveClass(double effectiveWidth) {
        if (effectiveWidth < SMALL_SCREEN_WIDTH) {
            return SMALL_SCREEN_CLASS;
        } else if (effectiveWidth >= EXTRA_LARGE_SCREEN_WIDTH) {
            return EXTRA_LARGE_SCREEN_CLASS;
        } else if (effectiveWidth >= LARGE_SCREEN_WIDTH) {
            return LARGE_SCREEN_CLASS;
        }
        return null; // Default/medium size - no class needed
    }
    
    /**
     * Get the detected system scale factor
     * @return System DPI scale factor (1.0 = 100%, 1.5 = 150%, etc.)
     */
    public static double getSystemScaleFactor() {
        return systemScaleDetected ? detectedSystemScale : 1.0;
    }
    
    /**
     * Force refresh of system DPI detection
     * @param stage The stage to use for re-detection
     */
    public static void refreshSystemDPIDetection(Stage stage) {
        systemScaleDetected = false;
        detectSystemDPIScaling(stage);
    }
    
    /**
     * Get the current screen size category as a string
     * @param width Window width
     * @return Screen size category
     */
    public static String getScreenSizeCategory(double width) {
        double effectiveWidth = width * getSystemScaleFactor();
        if (effectiveWidth < SMALL_SCREEN_WIDTH) {
            return "Small";
        } else if (effectiveWidth >= EXTRA_LARGE_SCREEN_WIDTH) {
            return "Extra Large";
        } else if (effectiveWidth >= LARGE_SCREEN_WIDTH) {
            return "Large";
        } else {
            return "Medium";
        }
    }
    
    /**
     * Check if the current width is considered a small screen
     * @param width Window width
     * @return true if small screen
     */
    public static boolean isSmallScreen(double width) {
        return (width * getSystemScaleFactor()) < SMALL_SCREEN_WIDTH;
    }
    
    /**
     * Apply responsive styles to a specific scene
     * This can be called manually if needed
     * @param scene The scene to apply responsive styles to
     */
    public static void applyResponsiveStyles(Scene scene) {
        if (scene == null) {
            return;
        }
        
        Stage stage = (Stage) scene.getWindow();
        if (stage != null) {
            updateResponsiveClass(scene, stage.getWidth());
        }
    }
    
    /**
     * Get recommended font scale factor based on screen size and DPI
     * @param width Window width
     * @return Font scale factor
     */
    public static double getFontScaleFactor(double width) {
        double effectiveWidth = width * getSystemScaleFactor();
        double systemScale = getSystemScaleFactor();
        
        // Base scale factor on effective screen size
        double baseScale;
        if (effectiveWidth < SMALL_SCREEN_WIDTH) {
            baseScale = 0.85;
        } else if (effectiveWidth >= EXTRA_LARGE_SCREEN_WIDTH) {
            baseScale = 1.2;
        } else if (effectiveWidth >= LARGE_SCREEN_WIDTH) {
            baseScale = 1.1;
        } else {
            baseScale = 1.0;
        }
        
        // Adjust for high DPI displays to prevent overly large text
        if (systemScale > 1.3) {
            baseScale *= 0.9; // Slightly reduce scaling on high DPI
        }
        
        return baseScale;
    }
    
    /**
     * Get recommended minimum window dimensions for optimal display
     * Adjusted for current system DPI scaling
     * @return array with [width, height]
     */
    public static double[] getMinimumWindowSize() {
        double systemScale = getSystemScaleFactor();
        double baseWidth = 800;
        double baseHeight = 600;
        
        // Adjust minimum size based on system scaling
        if (systemScale > 1.3) {
            // On high DPI systems, we can use smaller logical sizes
            baseWidth = 700;
            baseHeight = 525;
        } else if (systemScale < 1.0) {
            // On low DPI systems, use larger logical sizes
            baseWidth = 900;
            baseHeight = 675;
        }
        
        return new double[]{baseWidth, baseHeight};
    }
    
    /**
     * Force refresh of responsive styles
     * Useful when programmatically changing UI elements
     * @param scene The scene to refresh
     */
    public static void refreshResponsiveStyles(Scene scene) {
        if (scene != null) {
            Platform.runLater(() -> {
                applyResponsiveStyles(scene);
            });
        }
    }
    
    /**
     * Get current system information for debugging
     * @return String with system scaling information
     */
    public static String getSystemScalingInfo() {
        StringBuilder info = new StringBuilder();
        info.append("System DPI Scaling Information:\n");
        info.append("- Detected Scale Factor: ").append(getSystemScaleFactor()).append("\n");
        info.append("- Primary Screen Bounds: ").append(Screen.getPrimary().getBounds()).append("\n");
        info.append("- Primary Screen Visual Bounds: ").append(Screen.getPrimary().getVisualBounds()).append("\n");
        info.append("- Output Scale: ").append(Screen.getPrimary().getOutputScaleX()).append("x").append(Screen.getPrimary().getOutputScaleY()).append("\n");
        info.append("- OS: ").append(System.getProperty("os.name")).append("\n");
        info.append("- Java Version: ").append(System.getProperty("java.version"));
        return info.toString();
    }
} 