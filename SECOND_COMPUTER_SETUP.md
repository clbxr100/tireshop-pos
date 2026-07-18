# Second Computer Setup Guide

## Prerequisites
- Both computers must be on the same network
- Current computer (192.168.1.100) must remain powered on to serve the database
- Java 11+ installed on the second computer

## Step 1: Prepare the Second Computer

1. **Download the application files** to the second computer
2. **Install Java 11+** if not already installed
3. **Configure the database connection** (see below)

## Step 2: Database Configuration

Your current computer is already configured as a database server at IP `192.168.1.100`.

Create a `database.properties` file on the second computer with:
```properties
# Network database configuration - points to your main computer
db.url=jdbc:h2:tcp://192.168.1.100:9092/./tireshop
db.user=sa
db.password=
```

## Step 3: Network Configuration

### On the MAIN computer (192.168.1.100):
1. **Windows Firewall**: Allow port 9092
   - Open Windows Firewall → Advanced Settings
   - New Inbound Rule → Port → TCP → 9092
   - Allow the connection

2. **Verify H2 Server is running**:
   - The H2 server should start automatically with your application
   - You should see a message like "TCP server running at tcp://192.168.1.100:9092"

### On the SECOND computer:
1. **Test connectivity**:
   - Open Command Prompt
   - Type: `telnet 192.168.1.100 9092`
   - If it connects, you're good to go

## Step 4: Application Setup

1. **Copy application files** to the second computer
2. **Use the same database.properties** as shown above
3. **Run the application** - it will connect to the main computer's database

## Troubleshooting

### Connection Issues:
- Ensure main computer's firewall allows port 9092
- Check that both computers are on the same network
- Verify the IP address is correct (run `ipconfig` on main computer)

### Performance:
- Both computers will share the same data in real-time
- Network speed affects performance - use wired connection if possible
- The main computer handles all database operations

## Important Notes:
- **Keep the main computer running** - it hosts the database
- **Backups are handled** by the main computer
- **Both computers will have identical data** at all times
- **No data conflicts** since they share the same database 