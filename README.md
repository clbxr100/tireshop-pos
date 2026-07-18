# Tire Shop POS System

A comprehensive Point of Sale (POS) system for tire shops with mechanic services, inventory management, and sales tracking.

## Features

- **Product Management**: Track tires, rims, and other automotive products
- **Service Management**: Configure and track mechanic services
- **Technician Management**: Assign technicians to service jobs
- **Customer Management**: Maintain customer records and vehicle history
- **Sales**: Process sales of both products and services
- **Inventory**: Real-time inventory tracking with barcode scanning support
- **Reporting**: Generate sales and inventory reports
- **Multi-Device Support**: Works across multiple computers with shared database
- **Mobile Integration**: Scan barcodes for inventory using mobile devices

## Deployment Instructions for Multiple Machines

### Building the Deployment Package

To create a deployable package:

1. Make sure you have Maven installed
2. Run the following command in the project directory:
   ```
   mvn clean package
   ```
3. This will create a self-contained JAR file in the `target` directory

### Option 1: Individual Installation on Each PC

1. Copy the entire project folder to a USB drive or network share
2. On each PC, run the `install-tireshop.bat` script
3. The application will be installed to `C:\TireShopPOS` and a desktop shortcut will be created
4. Each PC will maintain its own separate database

### Option 2: Shared Database Setup (Recommended)

For a multi-PC setup with synchronized data:

1. Complete individual installation on all PCs using `install-tireshop.bat`
2. Decide which PC will host the shared database or create a network share
3. On each PC, run the `setup-shared-db.bat` script
4. Enter the UNC path where the database should be stored (e.g., `\\SERVER\Share\TireShopDB`)
5. The script will configure all PCs to use the same database file

#### Network Requirements for Shared Database

- All PCs must have read/write access to the shared network location
- The shared folder should be accessible even if the host PC is restarted
- Regular backups of the shared database are recommended

### System Requirements

- Windows 10 or newer
- Java 11 or higher
- 4GB RAM minimum, 8GB recommended
- Network connectivity between PCs for shared database mode

## Barcode Scanning Features

The system supports multiple barcode scanning methods:

1. Camera-based scanning using your PC/laptop webcam
2. Photo upload of barcodes
3. Manual barcode entry

For best results, ensure your PC has a webcam with autofocus capabilities.

## Database Capacity

The H2 database can handle:
- Thousands of products
- Complete sales history
- Customer records
- Tire specifications

For very large operations, the database can eventually be migrated to MySQL or PostgreSQL.

## Setup Instructions

1. **Install Java**: Ensure you have Java 11+ installed on your system
2. **Download the Application**: Get the latest release from the releases page
3. **Database Setup**: 
   - By default, the application uses an embedded H2 database
   - For production use, configure an external database in `application.properties`
4. **Run the Application**:
   - Windows: Double-click the .jar file or run `java -jar tire-shop-pos.jar`
   - macOS/Linux: Run `java -jar tire-shop-pos.jar`

## Developer Setup

1. Clone this repository
2. Open the project in your IDE (IntelliJ IDEA, Eclipse, etc.)
3. Build with Maven: `mvn clean install`
4. Run the main class: `com.tireshop.Main`

## Project Structure

- `model`: Data model classes representing business entities
- `dao`: Data Access Objects for database operations
- `service`: Business logic services
- `view`: JavaFX UI components
- `controller`: UI controllers
- `util`: Utility classes for barcode scanning, database, etc.

## Mobile Integration

For mobile barcode scanning:
1. Install a barcode scanning app that can export data
2. Configure the app to send scan results to the POS system
3. Connect the mobile device to the same network as the POS system

## Customization

The system can be customized by:
- Editing `application.properties` for system settings
- Adding new service types in the Services tab
- Configuring tax rates and receipt formats
- Setting up user roles and permissions

## Support

For issues or feature requests, please create an issue in the project repository.

## License

This project is licensed under the MIT License - see the LICENSE file for details. 