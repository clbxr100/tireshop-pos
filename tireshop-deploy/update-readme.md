# IMPORTANT UPDATE FOR README

This text contains information to update in the README.md file.

## Installation Instructions (Updated)

1. **Download & Run the installation package**
   - Execute `install-tireshop.bat` to install the application
   - A desktop shortcut will be created automatically
   - The default installation location is `C:\TireShopPOS`

2. **Add Required Dependencies**
   - Run `download-dependencies.bat` to download all required Java libraries
   - This downloads Hibernate, JPA, ZXing, and other needed libraries
   - This step is essential and must be done before running the application

3. **Install JavaFX (required for the application's UI)**
   - Run `download-javafx.bat` to automatically download and install JavaFX
   - This only needs to be done once per workstation

4. **Verify the installation**
   - Run `verify-javafx.bat` to ensure JavaFX is properly installed
   - Run `test-javafx.bat` to test if the JavaFX UI works correctly

## Troubleshooting

If compilation fails with errors about missing classes or packages:
1. Make sure you've run `download-dependencies.bat`
2. Check that all JAR files were downloaded to the lib directory
3. If some dependencies are missing, run the download script again or download the missing files manually 