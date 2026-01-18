@echo off
REM Deploy Self-Service iVALT MFA

set KEYCLOAK_HOME=e:\VKS\Ivalt\Keyclock\keycloak-26.5.0
set SOURCE_DIR=e:\VKS\Ivalt\Keyclock\Github\keycloak

echo ========================================
echo Deploy Self-Service iVALT MFA
echo ========================================
echo.

REM Step 1: Compile
echo [1/5] Compiling iVALT code...
cd /d "%SOURCE_DIR%"
call compile-ivalt-only.bat
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Compilation failed!
    pause
    exit /b 1
)

REM Step 2: Create JAR
echo.
echo [2/5] Creating iVALT JAR...
call create-ivalt-jar.bat
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: JAR creation failed!
    pause
    exit /b 1
)

REM Step 3: Copy to Keycloak
echo.
echo [3/5] Copying JAR to Keycloak...
copy /Y "%SOURCE_DIR%\keycloak-ivalt-mfa.jar" "%KEYCLOAK_HOME%\providers\"
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Failed to copy JAR!
    pause
    exit /b 1
)

echo.
echo ========================================
echo ✓ Self-Service iVALT MFA deployed!
echo.
echo Next steps:
echo   1. Rebuild Keycloak: cd %KEYCLOAK_HOME%\bin ^&^& kc.bat build
echo   2. Restart server
echo   3. Check for "iVALT MFA (Self-Service)" in Admin Console
echo ========================================
echo.
pause
