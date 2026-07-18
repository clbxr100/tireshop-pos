# GitHub Auto-Update Setup Guide

## Overview
This guide will help you set up GitHub-based automatic updates for your Tire Shop POS system across both computers.

## What This System Does
- ✅ Automatically checks GitHub for code updates
- ✅ Downloads and builds new versions
- ✅ Backs up current version before updating
- ✅ Stops the application, updates files, and restarts automatically
- ✅ Works on both computers simultaneously
- ✅ Provides logging and error handling

## Prerequisites

### 1. GitHub Repository Setup
1. **Create a GitHub account** if you don't have one
2. **Create a new repository** for your POS system:
   - Go to github.com → New Repository
   - Name it something like `tireshop-pos`
   - Make it private (recommended for business code)
3. **Upload your current code** to the repository

### 2. Both Computers Need:
- **Python 3.7+** installed with pip
- **Java 11+** installed
- **Maven** installed (for building)
- **Git** installed (optional, for advanced usage)

## Step-by-Step Setup

### Step 1: Upload Your Code to GitHub

If you haven't already, upload your current project:

```bash
# In your project directory
git init
git add .
git commit -m "Initial commit"
git remote add origin https://github.com/YOUR-USERNAME/tireshop-pos.git
git push -u origin main
```

### Step 2: Setup Auto-Updates on Main Computer

1. **Run the setup script:**
   ```
   setup-github-updates.bat
   ```

2. **Edit the configuration file** (`updater-config.json`):
   ```json
   {
     "github": {
       "owner": "YOUR-GITHUB-USERNAME",
       "repo": "tireshop-pos",
       "branch": "main",
       "token": "YOUR-GITHUB-TOKEN-IF-PRIVATE"
     },
     "app": {
       "jar_path": "target/tireshop-pos.jar",
       "backup_dir": "backups",
       "restart_command": "java -jar tireshop-pos.jar",
       "stop_command": "taskkill /f /im java.exe",
       "process_name": "java.exe"
     },
     "update": {
       "check_interval": 300,
       "auto_restart": true,
       "backup_before_update": true,
       "files_to_update": [
         "target/tireshop-pos.jar",
         "src/main/resources/config.properties",
         "src/main/resources/styles/modern.css"
       ]
     }
   }
   ```

3. **Test the setup:**
   ```
   check-updates.bat
   ```

### Step 3: Setup Auto-Updates on Second Computer

1. **Copy the update files** to the second computer:
   - `github-updater.py`
   - `setup-github-updates.bat`
   - `check-updates.bat`
   - `update-now.bat`
   - `start-auto-updates.bat`

2. **Run the setup:**
   ```
   setup-github-updates.bat
   ```

3. **Use the same configuration** as the main computer

## Usage

### Manual Operations

- **Check for updates:**
  ```
  check-updates.bat
  ```

- **Update immediately:**
  ```
  update-now.bat
  ```

- **Start continuous monitoring:**
  ```
  start-auto-updates.bat
  ```

### Automatic Operations

To run continuous updates in the background:
1. Double-click `start-auto-updates.bat`
2. The system will check for updates every 5 minutes (configurable)
3. If updates are found, it will automatically:
   - Create a backup
   - Download the latest code
   - Build the application
   - Stop the current application
   - Update the files
   - Restart the application

## Making Updates

### To push updates to both computers:

1. **Make your changes** to the code
2. **Commit to GitHub:**
   ```bash
   git add .
   git commit -m "Description of your changes"
   git push origin main
   ```
3. **Both computers will automatically detect and apply the update** (if auto-update is running)

## GitHub Token Setup (for Private Repositories)

If your repository is private, you need a GitHub token:

1. Go to GitHub → Settings → Developer settings → Personal access tokens
2. Generate a new token with `repo` permissions
3. Copy the token and add it to your `updater-config.json`

## Firewall Configuration

### Main Computer (Database Server):
- **Allow port 9092** for database connections
- **Allow outbound HTTPS** for GitHub access

### Second Computer:
- **Allow outbound HTTPS** for GitHub access
- **Allow outbound port 9092** to connect to main computer

## Troubleshooting

### Common Issues:

1. **"Python not found"**
   - Install Python from python.org
   - Make sure "Add Python to PATH" is checked

2. **"Maven not found"**
   - Install Maven from maven.apache.org
   - Add to system PATH

3. **"Cannot connect to GitHub"**
   - Check internet connection
   - Verify GitHub repository URL
   - Check if repository is private (needs token)

4. **"Build failed"**
   - Check if all dependencies are available
   - Verify Java and Maven are properly installed

5. **"Application won't restart"**
   - Check if the process is fully stopped
   - Verify Java path is correct
   - Check application logs

### Log Files:
- **Update logs:** `updater.log`
- **Application logs:** Check console output
- **Backup directory:** `backups/`

## Security Considerations

1. **Keep your GitHub token secure** - don't share it
2. **Use private repositories** for business code
3. **Regular backups** are created automatically
4. **Test updates** in a development environment first

## Advanced Configuration

### Custom Update Intervals:
Edit `updater-config.json`:
```json
"update": {
  "check_interval": 600,  // 10 minutes
  "auto_restart": true,
  "backup_before_update": true
}
```

### Custom Files to Update:
Add more files to monitor:
```json
"files_to_update": [
  "target/tireshop-pos.jar",
  "src/main/resources/config.properties",
  "src/main/resources/styles/modern.css",
  "database.properties",
  "custom-config.json"
]
```

## Workflow Example

1. **Developer makes changes** on development computer
2. **Push to GitHub:** `git push origin main`
3. **Both POS computers automatically:**
   - Detect the update
   - Create backup
   - Download new code
   - Build application
   - Stop current app
   - Update files
   - Restart app
4. **Both computers are now running the latest version**

## Support

If you encounter issues:
1. Check the `updater.log` file for error messages
2. Verify your GitHub configuration
3. Test manual updates with `update-now.bat`
4. Check network connectivity between computers

This system provides a professional, automated deployment solution that will keep both computers synchronized with your latest code changes! 