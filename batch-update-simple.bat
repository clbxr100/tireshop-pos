@echo off
echo Starting Batch Update Tool...
echo.
echo NOTE: Make sure the TireRack service is running for best results.
echo       (cd tirerack-service && npm start)
echo.

java -cp "target/tireshop-1.0-SNAPSHOT.jar;lib/*" com.tireshop.util.BatchUpdateTool

pause 