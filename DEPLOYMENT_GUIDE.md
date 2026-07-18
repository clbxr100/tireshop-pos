# Tire Shop POS - Multi-Computer Deployment Guide

## Overview
This guide will help you deploy the Tire Shop POS system on multiple computers using a shared database.

## Architecture Options

### Option 1: One Computer as Server + Database (Recommended for Small Shops)
- Computer 1: Runs both the database server and POS application
- Computer 2 & 3: Run only the POS application, connecting to Computer 1's database

### Option 2: Dedicated Database Server
- Server: Runs only the database (can be a small PC or server)
- All 3 Computers: Run the POS application, connecting to the server

## Step-by-Step Deployment

### Step 1: Choose and Install Database Server

#### Option A: PostgreSQL (Recommended)
1. **On the server computer**, download PostgreSQL from https://www.postgresql.org/download/windows/
2. Install PostgreSQL (remember the password you set for 'postgres' user)
3. Create database and user:
   ```sql
   -- Connect as postgres user
   CREATE DATABASE tireshop_db;
   CREATE USER tireshop WITH PASSWORD 'your_secure_password';
   GRANT ALL PRIVILEGES ON DATABASE tireshop_db TO tireshop;
   ```

#### Option B: MySQL
1. **On the server computer**, download MySQL from https://dev.mysql.com/downloads/installer/
2. Install MySQL Server
3. Create database and user:
   ```sql
   CREATE DATABASE tireshop_db;
   CREATE USER 'tireshop'@'%' IDENTIFIED BY 'your_secure_password';
   GRANT ALL PRIVILEGES ON tireshop_db.* TO 'tireshop'@'%';
   FLUSH PRIVILEGES;
   ```

### Step 2: Configure Firewall
On the database server computer:
1. Open Windows Firewall
2. Add inbound rule for:
   - PostgreSQL: Port 5432
   - MySQL: Port 3306
3. Allow connections from your local network only

### Step 3: Build the Application
On your development computer:
```bash
mvn clean package
```
This creates `target/tire-shop-pos-1.0-jar-with-dependencies.jar`

### Step 4: Prepare Deployment Package
Create a folder with:
```
TireShopPOS/
├── tire-shop-pos.jar (renamed from tire-shop-pos-1.0-jar-with-dependencies.jar)
├── database.properties
├── run.bat
└── README.txt
```

### Step 5: Configure database.properties
Edit `database.properties` for each computer:

**For PostgreSQL:**
```properties
database.type=postgresql
database.url=jdbc:postgresql://SERVER_IP:5432/tireshop_db
database.username=tireshop
database.password=your_secure_password
database.pool.size=10
database.show_sql=false
```

**For MySQL:**
```properties
database.type=mysql
database.url=jdbc:mysql://SERVER_IP:3306/tireshop_db?useSSL=false&serverTimezone=UTC
database.username=tireshop
database.password=your_secure_password
database.pool.size=10
database.show_sql=false
```

Replace `SERVER_IP` with the actual IP address of your database server (e.g., 192.168.1.100)

### Step 6: Create run.bat
```batch
@echo off
java -jar tire-shop-pos.jar
pause
```

### Step 7: Deploy to Each Computer
1. Copy the TireShopPOS folder to each computer
2. Ensure Java 11+ is installed on each computer
3. Edit `database.properties` with correct server IP
4. Double-click `run.bat` to start

## Network Configuration

### Find Server IP Address
On the database server computer:
1. Open Command Prompt
2. Type `ipconfig`
3. Look for "IPv4 Address" (e.g., 192.168.1.100)

### Static IP (Recommended)
Set a static IP for the database server:
1. Network Settings → Change adapter options
2. Right-click your network → Properties
3. Internet Protocol Version 4 → Properties
4. Use the following IP address:
   - IP: 192.168.1.100 (example)
   - Subnet: 255.255.255.0
   - Gateway: 192.168.1.1 (your router)

## Data Migration from H2

If you have existing data in H2:

1. Export data using H2 Console:
   ```sql
   SCRIPT TO 'backup.sql';
   ```

2. Import to PostgreSQL/MySQL:
   - Modify the SQL file for syntax compatibility
   - Use pgAdmin (PostgreSQL) or MySQL Workbench to import

## Backup Strategy

### Automated Daily Backups
Create a scheduled task on the database server:

**PostgreSQL backup script (backup.bat):**
```batch
@echo off
set PGPASSWORD=your_password
"C:\Program Files\PostgreSQL\15\bin\pg_dump" -U tireshop -h localhost tireshop_db > "C:\Backups\tireshop_%date:~-4,4%%date:~-10,2%%date:~-7,2%.sql"
```

**MySQL backup script:**
```batch
@echo off
"C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump" -u tireshop -pyour_password tireshop_db > "C:\Backups\tireshop_%date:~-4,4%%date:~-10,2%%date:~-7,2%.sql"
```

## Troubleshooting

### Connection Issues
1. Check firewall settings
2. Verify database service is running
3. Test connection: `telnet SERVER_IP 5432` (or 3306 for MySQL)
4. Check database.properties has correct IP

### Performance Issues
1. Increase connection pool size in database.properties
2. Ensure good network connection between computers
3. Consider upgrading to gigabit network

### Data Sync Issues
- All computers share the same database, so data is always synchronized
- If you see stale data, check network connectivity

## Security Recommendations

1. **Use strong passwords** for database users
2. **Limit database access** to local network only
3. **Regular backups** to external drive or cloud
4. **Windows user accounts** with passwords on each POS computer
5. **Antivirus software** on all computers

## Maintenance

### Weekly Tasks
- Check backup files
- Review error logs
- Verify all computers can connect

### Monthly Tasks
- Test restore from backup
- Update Windows and Java
- Clean up old log files

### Yearly Tasks
- Review and update passwords
- Archive old transaction data
- Hardware maintenance

## Support Contacts
- Database Issues: [Your IT support]
- Application Issues: [Developer contact]
- Network Issues: [Network administrator]

## Quick Reference

### Start Services
- PostgreSQL: `net start postgresql-x64-15`
- MySQL: `net start MySQL80`

### Stop Services
- PostgreSQL: `net stop postgresql-x64-15`
- MySQL: `net stop MySQL80`

### Check Logs
- PostgreSQL: `C:\Program Files\PostgreSQL\15\data\log\`
- MySQL: `C:\ProgramData\MySQL\MySQL Server 8.0\Data\*.err`
- Application: Check console output or redirect to file 