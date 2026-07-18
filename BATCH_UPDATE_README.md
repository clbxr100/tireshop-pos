# Tire Shop Batch Update Tool

## Overview

The Batch Update Tool is a utility for updating product information in the Tire Shop POS system. It can automatically fetch and update missing tire specifications from the TireRack service.

## Features

1. **Update All Products** - Batch update all tire products with missing size information
2. **Update Specific Product** - Update a single product by barcode
3. **Show Missing Information** - Display all products that have incomplete data
4. **Test TireRack Service** - Verify the TireRack service connection

## Prerequisites

1. Java 11 or higher installed
2. The main POS application must be built (`mvn package`)
3. TireRack service should be running for best results (optional but recommended)

## Starting the TireRack Service

For best results, start the TireRack service before running the batch update:

```bash
cd tirerack-service
npm install  # First time only
npm start
```

The service will run on port 3001.

## Running the Batch Update Tool

### Option 1: Using the batch script (Recommended)

```bash
batch-update.bat
```

This script will:
- Check if the TireRack service is running
- Warn you if it's not available
- Start the batch update tool

### Option 2: Using the simple batch script

```bash
batch-update-simple.bat
```

This is a simpler version without curl dependency.

### Option 3: Running directly with Java

```bash
java -cp "target/tireshop-1.0-SNAPSHOT.jar;lib/*" com.tireshop.util.BatchUpdateTool
```

## Menu Options

### 1. Update all products with missing size information

This option will:
- Scan all tire products in the database
- Skip products that already have complete information
- Look up missing data from the TireRack service
- Update the following fields if missing:
  - Size
  - Manufacturer
  - Tire Type
  - Speed Rating
  - Load Rating
  - UTQG Ratings
  - Description

### 2. Update a specific product by barcode

This option allows you to:
- Enter a specific barcode
- View current product information
- Update missing fields from the TireRack service
- See the updated information immediately

### 3. Show products with missing information

This option will display:
- All tire products with incomplete data
- Which fields are missing for each product
- The barcode for each product (if available)

### 4. Test TireRack service connection

This option helps troubleshoot by:
- Testing a barcode lookup
- Showing what data the service returns
- Verifying the service is accessible

## What Gets Updated

The tool updates the following fields when they are missing:

- **Size** - Tire size (e.g., "225/65R17")
- **Manufacturer** - Brand name (e.g., "Mastercraft", "Cooper")
- **Tire Type** - Category (e.g., "All-Season", "Performance", "Winter")
- **Speed Rating** - Maximum speed capability (e.g., "H", "V", "W")
- **Load Rating** - Weight capacity (e.g., "91", "94", "98")
- **UTQG Ratings** - Treadwear, Traction, and Temperature ratings
- **Description** - Detailed product description

## Known Barcodes

The TireRack service recognizes these barcodes:

**Mastercraft:**
- 029142803881
- 029142738251
- 029142901878
- 029142931935
- 029142810193
- 029142364801

**Cooper:**
- 086699137876
- 086699372055
- 086699296115
- 086699216779

**Goodyear:**
- 086699783646

## Troubleshooting

### TireRack service not running
- Start the service: `cd tirerack-service && npm start`
- The tool will still work but won't fetch new data

### No data returned for a barcode
- The barcode may not be in the TireRack database
- Try using the test option to verify the service response

### Rate limiting errors
- The tool includes a 500ms delay between API calls
- If you see rate limit errors, wait a moment and try again

## Technical Details

- The tool uses the existing database connection from the POS system
- Updates are performed through the ProductDao
- All changes are logged to the console
- The tool respects existing data and only updates missing fields 