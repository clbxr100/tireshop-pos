# Tire Shop POS System

This is a Point of Sale (POS) system designed specifically for tire shops, with barcode scanning capabilities.

## System Requirements

- Windows 7/8/10/11
- Java 11 or higher (OpenJDK or Oracle JDK)
- 4GB RAM minimum (8GB recommended)
- 500MB disk space

## Installation Instructions

1. **Download & Run the installation package**
   - Execute `install-tireshop.bat` to install the application
   - A desktop shortcut will be created automatically
   - The default installation location is `C:\TireShopPOS`

2. **Add Required Dependencies**
   - Run `download-dependencies.bat` to download all required Java libraries
   - This downloads Hibernate, JPA, ZXing, and other needed libraries
   - This step is essential and must be done before running the application

3. **Install JavaFX (required for the application's UI)**
   - Run `download-javafx.bat` to automatically download and install JavaFX
   - This only needs to be done once per workstation

4. **Verify the installation**
   - Run `verify-javafx.bat` to ensure JavaFX is properly installed
   - Run `test-javafx.bat` to test if the JavaFX UI works correctly

## Running the Application

1. Double-click the desktop icon "Tire Shop POS"
   - OR -
2. Run `run-tireshop.bat` in the installation directory

## Multi-Workstation Setup

For tire shops with multiple workstations, you can set up:

1. **Individual Databases (Default)**
   - Each workstation maintains its own inventory and sales data
   - Simple, no additional configuration needed

2. **Shared Database**
   - Run `setup-shared-db.bat` on one workstation to create a shared database
   - All workstations will use the same inventory and sales data
   - Requires network connectivity between workstations

## Troubleshooting

If the application fails to start:

1. **Java issues**
   - Ensure Java 11+ is installed on your system
   - Check that it's in your system PATH or in a standard location

2. **JavaFX issues**
   - Run `verify-javafx.bat` to check if JavaFX is properly installed
   - If missing, run `download-javafx.bat` again

3. **Dependency issues**
   - If you see compilation errors about missing classes/packages
   - Run `download-dependencies.bat` to download all required dependencies
   - Check that the JAR files are in the lib directory

4. **Application log**
   - Check `app.log` in the installation directory for detailed error messages

## Support

For technical support, contact [support email/contact information].

## License

[License information] 