#!/usr/bin/env python3
"""
GitHub Auto-Update System for Tire Shop POS
Automatically pulls updates from GitHub and restarts the application
"""

import os
import sys
import json
import subprocess
import time
import requests
import shutil
import tempfile
from datetime import datetime
from pathlib import Path
import hashlib
import zipfile

class GitHubUpdater:
    def __init__(self, config_file="updater-config.json"):
        self.config_file = config_file
        self.config = self.load_config()
        self.log_file = "updater.log"
        
    def load_config(self):
        """Load updater configuration"""
        if os.path.exists(self.config_file):
            with open(self.config_file, 'r') as f:
                return json.load(f)
        else:
            # Create default config
            default_config = {
                "github": {
                    "owner": "your-username",
                    "repo": "tireshop-pos",
                    "branch": "main",
                    "token": ""  # Optional: for private repos
                },
                "app": {
                    "jar_path": "target/tireshop-pos.jar",
                    "backup_dir": "backups",
                    "restart_command": "java -jar tireshop-pos.jar",
                    "stop_command": "taskkill /f /im java.exe",
                    "process_name": "java.exe"
                },
                "update": {
                    "check_interval": 300,  # seconds
                    "auto_restart": True,
                    "backup_before_update": True,
                    "files_to_update": [
                        "target/tireshop-pos.jar",
                        "src/main/resources/config.properties",
                        "src/main/resources/styles/modern.css"
                    ]
                }
            }
            
            with open(self.config_file, 'w') as f:
                json.dump(default_config, f, indent=2)
            
            self.log(f"Created default config file: {self.config_file}")
            self.log("Please update the GitHub configuration in the config file")
            return default_config
    
    def log(self, message):
        """Log messages with timestamp"""
        timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        log_entry = f"[{timestamp}] {message}"
        print(log_entry)
        
        with open(self.log_file, 'a') as f:
            f.write(log_entry + "\n")
    
    def get_latest_commit(self):
        """Get the latest commit SHA from GitHub"""
        try:
            owner = self.config["github"]["owner"]
            repo = self.config["github"]["repo"]
            branch = self.config["github"]["branch"]
            
            url = f"https://api.github.com/repos/{owner}/{repo}/commits/{branch}"
            
            headers = {}
            if self.config["github"]["token"]:
                headers["Authorization"] = f"token {self.config['github']['token']}"
            
            response = requests.get(url, headers=headers, timeout=30)
            response.raise_for_status()
            
            commit_data = response.json()
            return commit_data["sha"], commit_data["commit"]["message"]
            
        except Exception as e:
            self.log(f"Error fetching latest commit: {e}")
            return None, None
    
    def get_current_commit(self):
        """Get the current commit SHA from local file"""
        commit_file = "current_commit.txt"
        if os.path.exists(commit_file):
            with open(commit_file, 'r') as f:
                return f.read().strip()
        return None
    
    def save_current_commit(self, commit_sha):
        """Save the current commit SHA to local file"""
        with open("current_commit.txt", 'w') as f:
            f.write(commit_sha)
    
    def download_update(self, commit_sha):
        """Download the latest code from GitHub"""
        try:
            owner = self.config["github"]["owner"]
            repo = self.config["github"]["repo"]
            branch = self.config["github"]["branch"]
            
            # Download as ZIP
            url = f"https://github.com/{owner}/{repo}/archive/{branch}.zip"
            
            self.log(f"Downloading update from {url}")
            
            response = requests.get(url, timeout=300)
            response.raise_for_status()
            
            # Save to temporary file
            temp_dir = tempfile.mkdtemp()
            zip_path = os.path.join(temp_dir, "update.zip")
            
            with open(zip_path, 'wb') as f:
                f.write(response.content)
            
            # Extract ZIP
            extract_path = os.path.join(temp_dir, "extracted")
            with zipfile.ZipFile(zip_path, 'r') as zip_ref:
                zip_ref.extractall(extract_path)
            
            # Find the extracted folder (usually repo-branch)
            extracted_folders = os.listdir(extract_path)
            if extracted_folders:
                source_path = os.path.join(extract_path, extracted_folders[0])
                return source_path
            
            return None
            
        except Exception as e:
            self.log(f"Error downloading update: {e}")
            return None
    
    def backup_current_version(self):
        """Backup current version before update"""
        try:
            backup_dir = self.config["app"]["backup_dir"]
            os.makedirs(backup_dir, exist_ok=True)
            
            timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
            backup_name = f"backup_{timestamp}"
            backup_path = os.path.join(backup_dir, backup_name)
            
            # Copy important files
            files_to_backup = [
                "target/tireshop-pos.jar",
                "database.properties",
                "current_commit.txt"
            ]
            
            os.makedirs(backup_path, exist_ok=True)
            
            for file_path in files_to_backup:
                if os.path.exists(file_path):
                    dest_path = os.path.join(backup_path, os.path.basename(file_path))
                    shutil.copy2(file_path, dest_path)
            
            self.log(f"Backup created: {backup_path}")
            return backup_path
            
        except Exception as e:
            self.log(f"Error creating backup: {e}")
            return None
    
    def build_application(self, source_path):
        """Build the application from source"""
        try:
            self.log("Building application...")
            
            # Change to source directory
            old_cwd = os.getcwd()
            os.chdir(source_path)
            
            # Run Maven build
            result = subprocess.run(
                ["mvn", "clean", "package", "-DskipTests"],
                capture_output=True,
                text=True,
                timeout=600  # 10 minutes timeout
            )
            
            os.chdir(old_cwd)
            
            if result.returncode == 0:
                self.log("Build successful")
                return True
            else:
                self.log(f"Build failed: {result.stderr}")
                return False
                
        except Exception as e:
            self.log(f"Error building application: {e}")
            return False
    
    def update_files(self, source_path):
        """Update application files"""
        try:
            files_to_update = self.config["update"]["files_to_update"]
            
            for file_path in files_to_update:
                source_file = os.path.join(source_path, file_path)
                dest_file = file_path
                
                if os.path.exists(source_file):
                    # Create destination directory if needed
                    os.makedirs(os.path.dirname(dest_file), exist_ok=True)
                    
                    # Copy file
                    shutil.copy2(source_file, dest_file)
                    self.log(f"Updated: {file_path}")
                else:
                    self.log(f"Warning: Source file not found: {source_file}")
            
            return True
            
        except Exception as e:
            self.log(f"Error updating files: {e}")
            return False
    
    def stop_application(self):
        """Stop the running application"""
        try:
            self.log("Stopping application...")
            
            # Try graceful shutdown first
            if self.config["app"]["stop_command"]:
                subprocess.run(self.config["app"]["stop_command"], shell=True)
            
            # Wait a moment
            time.sleep(2)
            
            # Force kill if still running
            subprocess.run(f"taskkill /f /im {self.config['app']['process_name']}", shell=True)
            
            time.sleep(1)
            self.log("Application stopped")
            return True
            
        except Exception as e:
            self.log(f"Error stopping application: {e}")
            return False
    
    def start_application(self):
        """Start the application"""
        try:
            self.log("Starting application...")
            
            # Start application in background
            subprocess.Popen(
                self.config["app"]["restart_command"],
                shell=True,
                creationflags=subprocess.CREATE_NEW_CONSOLE
            )
            
            time.sleep(3)
            self.log("Application started")
            return True
            
        except Exception as e:
            self.log(f"Error starting application: {e}")
            return False
    
    def check_for_updates(self):
        """Check if updates are available"""
        self.log("Checking for updates...")
        
        latest_commit, commit_message = self.get_latest_commit()
        if not latest_commit:
            return False
        
        current_commit = self.get_current_commit()
        
        if current_commit == latest_commit:
            self.log("No updates available")
            return False
        
        self.log(f"Update available: {latest_commit[:8]} - {commit_message}")
        return True
    
    def perform_update(self):
        """Perform the complete update process"""
        self.log("Starting update process...")
        
        # Check for updates
        latest_commit, commit_message = self.get_latest_commit()
        if not latest_commit:
            self.log("Failed to check for updates")
            return False
        
        current_commit = self.get_current_commit()
        if current_commit == latest_commit:
            self.log("Already up to date")
            return True
        
        # Backup current version
        if self.config["update"]["backup_before_update"]:
            backup_path = self.backup_current_version()
            if not backup_path:
                self.log("Failed to create backup, aborting update")
                return False
        
        # Download update
        source_path = self.download_update(latest_commit)
        if not source_path:
            self.log("Failed to download update")
            return False
        
        try:
            # Build application
            if not self.build_application(source_path):
                self.log("Failed to build application")
                return False
            
            # Stop application
            if self.config["update"]["auto_restart"]:
                self.stop_application()
            
            # Update files
            if not self.update_files(source_path):
                self.log("Failed to update files")
                return False
            
            # Save new commit
            self.save_current_commit(latest_commit)
            
            # Restart application
            if self.config["update"]["auto_restart"]:
                self.start_application()
            
            self.log(f"Update completed successfully: {latest_commit[:8]}")
            return True
            
        finally:
            # Clean up temporary files
            if os.path.exists(source_path):
                shutil.rmtree(os.path.dirname(source_path))
    
    def run_update_service(self):
        """Run the update service continuously"""
        self.log("Starting GitHub update service...")
        
        check_interval = self.config["update"]["check_interval"]
        
        while True:
            try:
                if self.check_for_updates():
                    self.perform_update()
                
                self.log(f"Waiting {check_interval} seconds before next check...")
                time.sleep(check_interval)
                
            except KeyboardInterrupt:
                self.log("Update service stopped by user")
                break
            except Exception as e:
                self.log(f"Error in update service: {e}")
                time.sleep(60)  # Wait 1 minute before retrying

def main():
    import argparse
    
    parser = argparse.ArgumentParser(description="GitHub Auto-Update System")
    parser.add_argument("--config", default="updater-config.json", 
                       help="Configuration file path")
    parser.add_argument("--check", action="store_true", 
                       help="Check for updates only")
    parser.add_argument("--update", action="store_true", 
                       help="Perform update if available")
    parser.add_argument("--service", action="store_true", 
                       help="Run as continuous service")
    parser.add_argument("--setup", action="store_true", 
                       help="Setup configuration")
    
    args = parser.parse_args()
    
    updater = GitHubUpdater(args.config)
    
    if args.setup:
        print("Please edit the configuration file:", args.config)
        print("Set your GitHub repository details and application paths")
        return
    
    if args.check:
        updater.check_for_updates()
    elif args.update:
        updater.perform_update()
    elif args.service:
        updater.run_update_service()
    else:
        print("Use --help for usage information")

if __name__ == "__main__":
    main() 