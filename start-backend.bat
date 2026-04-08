@echo off
echo =======================================================
echo Starting KrushiKranti Backend...
echo =======================================================
echo Checking port 8080...

for /f "tokens=5" %%p in ('netstat -ano ^| findstr ":8080" ^| findstr "LISTENING"') do (
	echo Port 8080 is in use by PID %%p. Stopping it...
	taskkill /PID %%p /F >nul 2>&1
)

echo Using JDK 21 (since Java 25 is currently incompatible with Lombok)
echo Using Maven 3.9.6

set JAVA_HOME=C:\jdk21\jdk-21.0.2
"C:\apache-maven-3.9.6\bin\mvn.cmd" spring-boot:run
