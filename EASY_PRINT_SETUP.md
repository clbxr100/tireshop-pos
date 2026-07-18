# Easy Print Setup - Auto-Start with POS

## ✅ **Solution: Print Server Auto-Starts with POS**

The print server now automatically starts when you run `start-pos.bat` on the computer with the printer!

## 🚀 **Quick Setup (2 Steps)**

### **Step 1: On POS Computer (192.168.1.59)**
1. Copy this entire `pos` folder to the POS computer
2. **Keep the `enable-print-server.txt` file** (it's already included)
3. Run: `start-pos.bat`

### **Step 2: On Client Computers**
1. Copy this `pos` folder to client computers  
2. **Delete the `enable-print-server.txt` file** from client computers
3. Run: `start-pos-client.bat` (or `start-pos.bat`)

## 📋 **What Happens When You Start**

### **POS Computer (with enable-print-server.txt):**
```
Starting Tire Shop POS System...
Print server enabled for this computer
Starting Print Server...
Print server compiled successfully
Print server started in background (minimized window)
Starting TireRack API Service...
Starting POS Application...
```

### **Client Computer (without enable-print-server.txt):**
```
Starting Tire Shop POS System...
Print server disabled (create 'enable-print-server.txt' to enable)
Starting TireRack API Service...
Starting POS Application...
```

## 🎯 **How Printing Works**

1. **Client computer** tries to print → Sends to POS computer (192.168.1.59:8080)
2. **POS computer** receives request → Prints on local printer
3. **If connection fails** → Falls back to local printing

## 🔧 **Troubleshooting**

**Print server won't start?**
- Make sure Java JDK is installed (not just JRE)
- Check that `enable-print-server.txt` exists on POS computer
- Look for "Print Server" window (it's minimized)

**Client can't print to POS?**
- Verify POS computer IP is 192.168.1.59
- Check if print server is running: http://192.168.1.59:8080/status
- Check network connectivity: `ping 192.168.1.59`

**Wrong computer starting print server?**
- Delete `enable-print-server.txt` from client computers
- Only keep it on the POS computer with the physical printer

## 📁 **File Checklist**

**POS Computer (192.168.1.59):**
- ✅ `enable-print-server.txt` (KEEP THIS)
- ✅ Physical printer connected
- ✅ Run `start-pos.bat`

**Client Computers:**
- ❌ `enable-print-server.txt` (DELETE THIS)
- ❌ No printer needed
- ✅ Run `start-pos-client.bat`

## 🎉 **That's It!**

The system will automatically:
- Start print server on POS computer
- Handle remote printing from clients
- Stop everything when you close the POS application

**No more manual print server management needed!** 