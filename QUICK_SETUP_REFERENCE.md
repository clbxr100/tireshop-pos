# Quick Setup Reference

## 🗂️ Files Created for You

### Database Sharing Files:
- `SECOND_COMPUTER_SETUP.md` - Complete setup guide
- `setup-second-computer.bat` - Automated setup script

### GitHub Auto-Update Files:
- `github-updater.py` - Main update system
- `setup-github-updates.bat` - Initial setup
- `check-updates.bat` - Check for updates
- `update-now.bat` - Update immediately
- `start-auto-updates.bat` - Start continuous monitoring
- `install-update-service.bat` - Install as Windows service

### Documentation:
- `GITHUB_SETUP_GUIDE.md` - Complete GitHub setup guide
- `QUICK_SETUP_REFERENCE.md` - This file

## 🚀 Quick Start

### For Database Sharing (Second Computer):
1. Copy application files to second computer
2. Run: `setup-second-computer.bat`
3. Follow the prompts to configure database connection
4. Run: `run-pos.bat` to start the application

### For GitHub Auto-Updates (Both Computers):
1. Create GitHub repository and upload your code
2. Run: `setup-github-updates.bat`
3. Edit `updater-config.json` with your GitHub details
4. Test: `check-updates.bat`
5. Start auto-updates: `start-auto-updates.bat`

## 🔧 Configuration

### Database Connection:
```properties
db.url=jdbc:h2:tcp://192.168.1.100:9092/./tireshop
db.user=sa
db.password=
```

### GitHub Configuration:
```json
{
  "github": {
    "owner": "your-username",
    "repo": "tireshop-pos",
    "branch": "main"
  }
}
```

## 🛠️ Troubleshooting

### Database Issues:
- Ensure main computer (192.168.1.100) is running
- Check Windows Firewall allows port 9092
- Test connection: `telnet 192.168.1.100 9092`

### Update Issues:
- Check `updater.log` for error messages
- Verify GitHub repository URL
- Ensure Python and Maven are installed

## 📞 Support Commands

```bash
# Check updates
check-updates.bat

# Update now
update-now.bat

# Start continuous monitoring
start-auto-updates.bat

# Install as service (run as admin)
install-update-service.bat
```

## 🎯 Next Steps

1. **Test database sharing** between computers
2. **Set up GitHub repository** with your code
3. **Configure auto-updates** on both computers
4. **Test the complete workflow** end-to-end

✅ **You're all set!** Both computers will now share the same database and automatically update from GitHub. 