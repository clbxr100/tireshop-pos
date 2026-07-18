# Remote Printing Setup Guide

## Overview

This guide explains how to set up remote printing for your multi-computer POS system. The solution allows client computers to send print jobs to the POS computer where the physical printer is connected.

## Architecture

```
Client Computer (192.168.1.X) → HTTP Request → POS Computer (192.168.1.59) → Physical Printer
```

## Setup Steps

### Step 1: On the POS Computer (192.168.1.59)

1. **Start the Print Server**:
   ```
   Double-click: start-print-server.bat
   ```

2. **Keep the print server running** - You should see:
   ```
   Remote Print Server started on port 8080
   Ready to receive print jobs from client computers
   Press Ctrl+C to stop the server
   ```

3. **Configure Windows Firewall** (if needed):
   - Open Windows Defender Firewall
   - Click "Allow an app through firewall"
   - Add Java and allow for Private networks
   - Or allow port 8080 specifically

### Step 2: On Client Computers

The client computers will automatically try to send print jobs to the POS computer at 192.168.1.59:8080.

**No additional setup needed** - the PrinterService has been updated to:
1. Try remote printing first
2. Fall back to local printing if remote fails

### Step 3: Test the Setup

1. **Test connectivity** from a client computer:
   - Open a web browser
   - Go to: `http://192.168.1.59:8080/status`
   - You should see: "Print Server is running"

2. **Test printing** from the POS application:
   - Try to print a receipt
   - Check the logs for remote printing messages

## Troubleshooting

### Print Server Won't Start

**Error: "Address already in use"**
- Port 8080 is being used by another application
- Try stopping other services or use a different port
- Edit the RemotePrintServer.java file to change the port

**Error: "Java not found"**
- Install Java 11 or later on the POS computer
- Make sure Java is in the system PATH

### Client Can't Connect to Print Server

**Error: "Cannot connect to remote print server"**
- Check that the print server is running on the POS computer
- Verify the IP address (should be 192.168.1.59)
- Test connectivity: ping 192.168.1.59
- Check Windows Firewall settings on POS computer

**Error: "Connection timeout"**
- Network connectivity issues
- Firewall blocking the connection
- POS computer is not on the same network

### Prints Go to Wrong Printer

**Issue: Jobs print to unexpected printer**
- The print server uses the default printer on the POS computer
- Set the correct printer as default on the POS computer:
  - Control Panel → Devices and Printers
  - Right-click desired printer → "Set as default printer"

## Advanced Configuration

### Change Print Server Port

If port 8080 conflicts with other applications:

1. Edit `src/main/java/com/tireshop/util/RemotePrintServer.java`
2. Change `DEFAULT_PORT = 8080` to desired port
3. Edit `src/main/java/com/tireshop/util/PrinterService.java`
4. Change `remotePrintServerPort = 8080` to same port
5. Recompile: `mvn compile`

### Change POS Computer IP

If the POS computer IP changes:

1. Edit `src/main/java/com/tireshop/util/PrinterService.java`
2. Change `remotePrintServerIP = "192.168.1.59"` to new IP
3. Recompile: `mvn compile`

### Disable Remote Printing

To use only local printing:

1. Edit `src/main/java/com/tireshop/util/PrinterService.java`
2. Change `useRemotePrinting = true` to `useRemotePrinting = false`
3. Recompile: `mvn compile`

## Daily Operation

### Morning Startup
1. **On POS Computer**: Start print server first
   ```
   start-print-server.bat
   ```
2. **On POS Computer**: Start main POS application
   ```
   start-pos.bat
   ```
3. **On Client Computers**: Start POS application
   ```
   start-pos-client.bat
   ```

### Evening Shutdown
1. Stop POS applications on all computers
2. Stop print server (Ctrl+C in the print server window)

## Log Messages

**Successful Remote Printing:**
```
[PrinterService] Attempting remote printing to POS computer at 192.168.1.59
[PrinterService] Connecting to remote print server: http://192.168.1.59:8080/print
[PrinterService] Remote print response: Print job completed successfully
[PrinterService] Remote printing successful
```

**Fallback to Local Printing:**
```
[PrinterService] Cannot connect to remote print server at 192.168.1.59:8080
[PrinterService] Remote printing failed, falling back to local printing
[PrinterService] Attempting to print to: System Default
```

## Security Notes

- The print server only accepts connections from the local network
- No authentication is required (suitable for trusted local networks)
- Print content is sent unencrypted over HTTP
- For production use, consider adding HTTPS and authentication

## Support

If you encounter issues:
1. Check the log messages in the POS application
2. Verify network connectivity between computers
3. Ensure the print server is running on the POS computer
4. Test with a simple status check: http://192.168.1.59:8080/status 