@echo off
REM Script to create iVALT provider JAR file

echo Creating iVALT MFA Provider JAR...

REM Set paths
set KEYCLOAK_ROOT=e:\VKS\Ivalt\Keyclock\Github\keycloak
set JAR_DIR=%KEYCLOAK_ROOT%\ivalt-provider-jar
set OUTPUT_JAR=%KEYCLOAK_ROOT%\keycloak-ivalt-mfa.jar

REM Clean and create JAR directory
if exist "%JAR_DIR%" rmdir /s /q "%JAR_DIR%"
mkdir "%JAR_DIR%"
mkdir "%JAR_DIR%\org\keycloak\credential"
mkdir "%JAR_DIR%\org\keycloak\authentication\authenticators\browser"
mkdir "%JAR_DIR%\org\keycloak\authentication\requiredactions"
mkdir "%JAR_DIR%\org\keycloak\models\credential"
mkdir "%JAR_DIR%\META-INF\services"
mkdir "%JAR_DIR%\theme\base\login"

echo Copying class files...

REM Copy credential classes
xcopy /Y "%KEYCLOAK_ROOT%\services\target\classes\org\keycloak\credential\IvaltCredential*.class" "%JAR_DIR%\org\keycloak\credential\"

REM Copy authenticator classes
xcopy /Y "%KEYCLOAK_ROOT%\services\target\classes\org\keycloak\authentication\authenticators\browser\Ivalt*.class" "%JAR_DIR%\org\keycloak\authentication\authenticators\browser\"
xcopy /Y "%KEYCLOAK_ROOT%\services\target\classes\org\keycloak\authentication\authenticators\browser\Conditional*.class" "%JAR_DIR%\org\keycloak\authentication\authenticators\browser\"

REM Copy required action classes
xcopy /Y "%KEYCLOAK_ROOT%\services\target\classes\org\keycloak\authentication\requiredactions\ConfigureIvalt.class" "%JAR_DIR%\org\keycloak\authentication\requiredactions\"

REM Copy credential model classes
xcopy /Y "%KEYCLOAK_ROOT%\server-spi\target\classes\org\keycloak\models\credential\IvaltCredentialModel*.class" "%JAR_DIR%\org\keycloak\models\credential\"

REM Copy SPI registration files (iVALT entries only)
xcopy /Y "%KEYCLOAK_ROOT%\ivalt-spi\org.keycloak.authentication.AuthenticatorFactory" "%JAR_DIR%\META-INF\services\"
xcopy /Y "%KEYCLOAK_ROOT%\ivalt-spi\org.keycloak.authentication.RequiredActionFactory" "%JAR_DIR%\META-INF\services\"
xcopy /Y "%KEYCLOAK_ROOT%\ivalt-spi\org.keycloak.credential.CredentialProviderFactory" "%JAR_DIR%\META-INF\services\"

REM Copy Freemarker templates
echo Copying Freemarker templates...
xcopy /Y "%KEYCLOAK_ROOT%\themes\src\main\resources\theme\base\login\ivalt-*.ftl" "%JAR_DIR%\theme\base\login\"

REM Copy message properties
echo Copying message properties...
mkdir "%JAR_DIR%\theme\base\login\messages"
xcopy /Y "%KEYCLOAK_ROOT%\themes\src\main\resources\theme\base\login\messages\messages_en.properties" "%JAR_DIR%\theme\base\login\messages\"

echo Creating JAR file...

REM Create JAR using Java's jar command with manifest
cd /d "%JAR_DIR%"
jar cfm "%OUTPUT_JAR%" "%KEYCLOAK_ROOT%\ivalt-jar-manifest\MANIFEST.MF" .

if %ERRORLEVEL% EQU 0 (
    echo.
    echo ✓ JAR file created successfully: %OUTPUT_JAR%
    echo.
    echo File size:
    dir "%OUTPUT_JAR%" | findstr "keycloak-ivalt"
    echo.
    echo Next step: Rebuild Docker image with: docker-compose build
) else (
    echo ERROR: Failed to create JAR file!
    exit /b 1
)

REM Clean up
cd /d "%KEYCLOAK_ROOT%"
rmdir /s /q "%JAR_DIR%"
