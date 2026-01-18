@echo off
REM Quick compile script for iVALT classes only

echo Compiling iVALT classes...

REM Set paths
set KEYCLOAK_ROOT=e:\VKS\Ivalt\Keyclock\Github\keycloak
set MAVEN_CMD="C:\Users\vikesh sharma\.m2\wrapper\dists\apache-maven-3.9.8\8fbe43aa8cab8504a064f61871f5c160\bin\mvn.cmd"

REM Compile just server-spi (IvaltCredentialModel)
echo [1/2] Compiling server-spi...
cd /d "%KEYCLOAK_ROOT%\server-spi"
call %MAVEN_CMD% compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: server-spi compilation failed!
    exit /b 1
)

REM Compile just services (all other iVALT classes)
echo [2/2] Compiling services...
cd /d "%KEYCLOAK_ROOT%\services"
call %MAVEN_CMD% compile -DskipTests -q

if %ERRORLEVEL% NEQ 0 (
    echo ERROR: services compilation failed!
    exit /b 1
)

echo.
echo ✓ iVALT classes compiled successfully!
echo.
echo Compiled files:
dir /s /b "%KEYCLOAK_ROOT%\services\target\classes\*Ivalt*.class" 2>nul
dir /s /b "%KEYCLOAK_ROOT%\server-spi\target\classes\*Ivalt*.class" 2>nul

cd /d "%KEYCLOAK_ROOT%"
