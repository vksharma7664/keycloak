# iVALT MFA - Standalone Keycloak Setup Guide

## Quick Start (First Time Setup)

### Step 1: Download Keycloak

1. Go to https://www.keycloak.org/downloads
2. Download **Keycloak 24.0.5** (Server - ZIP distribution)
3. Extract to: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\`

### Step 2: Deploy iVALT Provider

1. Copy the iVALT JAR file:
   ```
   From: e:\VKS\Ivalt\Keyclock\Github\keycloak\keycloak-ivalt-mfa.jar
   To:   e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\providers\keycloak-ivalt-mfa.jar
   ```

2. Build Keycloak with iVALT provider:
   ```cmd
   cd e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\bin
   kc.bat build
   ```

### Step 3: Start Keycloak

```cmd
cd e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\bin
kc.bat start-dev
```

**First time only**: Create admin user when prompted:
- Username: `admin`
- Password: `admin` (or your choice)

### Step 4: Access Keycloak

- **Admin Console**: http://localhost:8080/admin
- Login with the credentials you created

### Step 5: Verify iVALT is Loaded

1. Go to **Authentication** → **Flows**
2. Click **Add execution** on any flow
3. **Look for "iVALT MFA"** in the provider dropdown
4. If you see it - **Success!** 🎉

---

## Development Workflow (Making Changes)

When you need to modify iVALT code:

### Option A: Manual Process

1. **Edit your code** in `e:\VKS\Ivalt\Keyclock\Github\keycloak\`
2. **Recompile**:
   ```cmd
   cd e:\VKS\Ivalt\Keyclock\Github\keycloak
   compile-ivalt-only.bat
   ```
3. **Rebuild JAR**:
   ```cmd
   create-ivalt-jar.bat
   ```
4. **Copy to Keycloak**:
   ```cmd
   copy keycloak-ivalt-mfa.jar e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\providers\
   ```
5. **Rebuild Keycloak**:
   ```cmd
   cd e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\bin
   kc.bat build
   ```
6. **Restart server**:
   ```cmd
   kc.bat start-dev
   ```

### Option B: Automated Script (Recommended)

Use the `deploy-to-standalone.bat` script (see below)

---

## Automated Deployment Script

Save this as `deploy-to-standalone.bat` in your source directory:

```batch
@echo off
REM Quick deployment script for iVALT to standalone Keycloak

set KEYCLOAK_HOME=e:\VKS\Ivalt\Keyclock\keycloak-24.0.5
set SOURCE_DIR=e:\VKS\Ivalt\Keyclock\Github\keycloak

echo ========================================
echo iVALT Quick Deploy to Standalone Keycloak
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
echo [2/5] Creating JAR...
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

REM Step 4: Rebuild Keycloak
echo.
echo [4/5] Rebuilding Keycloak...
cd /d "%KEYCLOAK_HOME%\bin"
call kc.bat build
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Keycloak build failed!
    pause
    exit /b 1
)

REM Step 5: Done
echo.
echo [5/5] ✓ Deployment complete!
echo.
echo ========================================
echo Next steps:
echo   1. Start Keycloak: cd %KEYCLOAK_HOME%\bin ^&^& kc.bat start-dev
echo   2. Access: http://localhost:8080/admin
echo ========================================
echo.
pause
```

**Usage**:
1. Edit the `KEYCLOAK_HOME` path if you installed Keycloak elsewhere
2. Run `deploy-to-standalone.bat` after making code changes
3. Start/restart Keycloak

---

## Configuring iVALT MFA

Once iVALT appears in Keycloak:

1. Go to **Authentication** → **Flows**
2. Create a new flow or edit "Browser" flow
3. Add **iVALT MFA** execution
4. Click ⚙️ (settings) and configure:
   - **API Base URL**: `https://api.ivalt.com`
   - **API Key**: Your iVALT API key
   - **API Timeout**: `300000` (5 minutes)
   - **Poll Interval**: `2000` (2 seconds)
5. Set requirement to **REQUIRED** or **ALTERNATIVE**
6. Save

## Testing iVALT MFA

1. Create a test user
2. Add required action: **Configure iVALT MFA**
3. Login as test user
4. Complete mobile number setup
5. Test authentication flow

---

## Troubleshooting

### iVALT doesn't appear in provider list

**Check**:
1. JAR file exists: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\providers\keycloak-ivalt-mfa.jar`
2. Keycloak was rebuilt: `kc.bat build`
3. Check logs: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\log\keycloak.log`

**Look for errors**:
```cmd
cd e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\log
findstr /i "ivalt error" keycloak.log
```

### Keycloak won't start

**Common issues**:
- Port 8080 already in use
- Java not installed or wrong version (need Java 17+)
- Database issues (delete `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\h2\` to reset)

### Changes not reflecting

**Solution**:
1. Stop Keycloak (Ctrl+C)
2. Delete: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\tmp\kc-gzip-cache\`
3. Run: `kc.bat build`
4. Restart: `kc.bat start-dev`

---

## File Locations

### Source Code
- **iVALT Code**: `e:\VKS\Ivalt\Keyclock\Github\keycloak\services\src\main\java\`
- **Compiled Classes**: `e:\VKS\Ivalt\Keyclock\Github\keycloak\services\target\classes\`
- **JAR File**: `e:\VKS\Ivalt\Keyclock\Github\keycloak\keycloak-ivalt-mfa.jar`

### Standalone Keycloak
- **Installation**: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\`
- **Providers**: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\providers\`
- **Logs**: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\log\`
- **Database**: `e:\VKS\Ivalt\Keyclock\keycloak-24.0.5\data\h2\`

---

## Next Steps

1. **Download Keycloak** from https://www.keycloak.org/downloads
2. **Follow Step 1-5** above
3. **Test iVALT** appears in admin console
4. **Configure and test** the MFA flow

Good luck! 🚀
