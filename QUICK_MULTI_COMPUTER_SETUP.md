# Quick Multi-Computer Setup Guide

## Step 1: Prepare the Server Computer (Main Computer)

1. **Build the application** (if not already done):
   ```
   mvn package
   ```

2. **Start the database server**:
   - Double-click `start-database-server.bat`
   - Keep this window open (minimize it)

3. **Get the server's IP address**:
   - Double-click `show-server-info.bat`
   - Note the IP address (usually starts with 192.168)

4. **Start the POS application**:
   - Double-click `start-pos.bat`

## Step 2: Setup Client Computers

1. **Copy the POS folder** to each client computer
   - Copy the entire `pos` folder to each computer
   - You can use a USB drive or network share

2. **Edit the client configuration**:
   - Open `start-pos-client.bat` in Notepad
   - Find the line: `set SERVER_IP=192.168.1.100`
   - Change it to your server's IP address
   - Save the file

3. **Start the client**:
   - Double-click `start-pos-client.bat`
   - It will check the connection and start the POS

## Important Notes

### On the Server Computer:
- Always start `start-database-server.bat` FIRST
- Keep the database server window open
- Then start the POS with `start-pos.bat`

### On Client Computers:
- Make sure the server is running first
- Use `start-pos-client.bat` (not start-pos.bat)
- All data is shared in real-time

### Firewall Settings:
If clients can't connect:
1. On the server, open Windows Defender Firewall
2. Click "Allow an app"
3. Add Java and allow for Private networks
4. Or temporarily disable firewall to test

### Backup Information:
- Automatic backups run every 5 hours
- Backups are saved in the `backups` folder on the server
- Only the server needs to run backups

## Troubleshooting

**"Cannot reach database server"**
- Check server IP address is correct
- Ensure server computer is on
- Check both computers are on same network
- Try pinging the server: `ping 192.168.x.x`

**"Could not find main class"**
- Run `mvn package` to build the application
- Make sure you copied the entire pos folder

**Performance is slow**
- Use wired network connection if possible
- Ensure good Wi-Fi signal if using wireless
- Close unnecessary programs

## Daily Operation

### Morning:
1. Turn on server computer
2. Run `start-database-server.bat`
3. Run `start-pos.bat`
4. Turn on client computers
5. Run `start-pos-client.bat` on each

### Evening:
1. Close POS on all client computers
2. Close POS on server
3. Close database server window
4. Backups run automatically

## Need Help?

- Check `MULTI_COMPUTER_SETUP.md` for detailed information
- Review log files in the application
- Ensure all computers have Java installed 