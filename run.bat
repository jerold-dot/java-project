@echo off
echo ========================================================
echo  Pharmacy Management System - MySQL Launch Script
echo ========================================================
echo.
echo Compiling project.java with MySQL JDBC Driver...
javac --release 17 -cp ".;mysql-connector-j-8.3.0.jar" project.java
if %errorlevel% neq 0 (
    echo.
    echo Compilation failed! Please check for errors above.
    pause
    exit /b %errorlevel%
)

echo.
echo Starting Pharmacy Inventory & Billing System...
java -cp ".;mysql-connector-j-8.3.0.jar" PharmacyManagementSystem
pause
