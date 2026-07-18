# Multi-Computer Setup - Quick Summary

## What I've Done

1. **Added Database Support**
   - Added PostgreSQL and MySQL drivers to the project
   - Created `database.properties` file for easy configuration
   - Updated DatabaseManager to support multiple database types

2. **Created Deployment Files**
   - `DEPLOYMENT_GUIDE.md` - Comprehensive deployment instructions
   - `create-installer.bat` - Script to build and package the application
   - `database.properties` - Configuration file for database connection

## Quick Setup Steps

### On Computer 1 (Database Server):
1. Install PostgreSQL or MySQL
2. Create database and user:
   ```sql
   CREATE DATABASE tireshop_db;
   CREATE USER tireshop WITH PASSWORD 'your_password';
   GRANT ALL PRIVILEGES ON DATABASE tireshop_db TO tireshop;
   ```
3. Configure Windows Firewall to allow database connections

### On All Computers:
1. Install Java 11+
2. Run `create-installer.bat` to build the deployment package
3. Copy the `TireShopPOS` folder from `deployment` directory
4. Edit `database.properties`:
   ```properties
   database.type=postgresql
   database.url=jdbc:postgresql://192.168.1.100:5432/tireshop_db
   database.username=tireshop
   database.password=your_password
   ```
5. Run `run.bat` to start the application

## Key Benefits
- All computers share the same database
- Real-time data synchronization
- No manual data syncing needed
- Centralized backups
- Multiple users can work simultaneously

## Important Notes
- Use static IP for database server
- Regular backups are essential
- Test on local network first
- Keep database password secure

## Files Created
- `src/main/resources/database.properties` - Database configuration
- `src/main/resources/hibernate.cfg.xml` - Hibernate configuration
- `DEPLOYMENT_GUIDE.md` - Full deployment guide
- `create-installer.bat` - Build and package script

## Next Steps
1. Choose between PostgreSQL (recommended) or MySQL
2. Set up database server on one computer
3. Run `create-installer.bat`
4. Deploy to all 3 computers
5. Test the setup 