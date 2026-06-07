@echo off
setlocal EnableDelayedExpansion

set JAVA_HOME=C:\Java\jdk21.0.10_7
set PATH=%JAVA_HOME%\bin;%PATH%

set MAVEN_VERSION=3.9.9
set WRAPPER_DIR=%~dp0.mvn\wrapper
set MAVEN_DIR=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%
set MAVEN_ZIP=%WRAPPER_DIR%\apache-maven-%MAVEN_VERSION%-bin.zip
set MAVEN_URL=https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/%MAVEN_VERSION%/apache-maven-%MAVEN_VERSION%-bin.zip

if not exist "%MAVEN_DIR%\bin\mvn.cmd" (
    echo [mvnw] Maven %MAVEN_VERSION% no encontrado. Descargando...
    powershell.exe -NoProfile -Command "Invoke-WebRequest -Uri '%MAVEN_URL%' -OutFile '%MAVEN_ZIP%' -UseBasicParsing"
    if errorlevel 1 (
        echo [mvnw] ERROR: No se pudo descargar Maven. Verifica tu conexion a internet.
        exit /b 1
    )
    powershell.exe -NoProfile -Command "Expand-Archive -Path '%MAVEN_ZIP%' -DestinationPath '%WRAPPER_DIR%' -Force"
    del /f /q "%MAVEN_ZIP%"
    echo [mvnw] Maven listo.
)

echo [mvnw] Java: %JAVA_HOME%
"%MAVEN_DIR%\bin\mvn.cmd" %*
endlocal
