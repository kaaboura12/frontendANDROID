@echo off
echo ========================================
echo   Google Sign-In Fix - Build Script
echo ========================================
echo.

echo [1/3] Cleaning project...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Clean failed!
    pause
    exit /b 1
)
echo ✓ Clean successful!
echo.

echo [2/3] Building project (this may take 2-5 minutes)...
call gradlew assembleDebug
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)
echo ✓ Build successful!
echo.

echo [3/3] Installation instructions:
echo.
echo 1. Uninstall the old app from your device/emulator
echo 2. In Android Studio: Click Run button (^>)
echo 3. Select your device
echo 4. Test Google Sign-In
echo.
echo ========================================
echo   Build Complete!
echo ========================================
pause

