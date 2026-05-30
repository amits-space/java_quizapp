@echo off
title MindGlow Quiz Portal Manager
color 0b
cls

:menu
cls
echo =======================================================================
echo              MINDGLOW QUIZ PORTAL - AUTOMATED SERVICES MANAGER         
echo =======================================================================
echo.
echo    [1] Start Quiz App (Full Build, Service Boot, Deploy and Open Browser)
echo    [2] Start Quiz App Fast (Deploy existing WAR, Service Boot, Open Browser)
echo    [3] Stop Application Services (Gracefully shuts down Tomcat and MySQL)
echo    [4] Check Status (Check if Tomcat and MySQL are currently running)
echo    [5] Exit
echo.
echo =======================================================================
set "choice="
set /p choice="Enter your choice (1-5): "

if "%choice%"=="1" goto option_build_run
if "%choice%"=="2" goto option_fast_run
if "%choice%"=="3" goto option_stop
if "%choice%"=="4" goto option_status
if "%choice%"=="5" goto option_exit

echo.
echo [ERROR] Invalid choice. Please try again.
ping 127.0.0.1 -n 3 >nul
goto menu

:option_build_run
cls
echo =======================================================================
echo    OPTION [1]: FULL BUILD, SERVICE BOOT, DEPLOY AND OPEN BROWSER
echo =======================================================================
echo.
call :sub_check_mysql
call :sub_check_tomcat
call :sub_build_project
call :sub_deploy_war
call :sub_launch_browser
echo.
echo [SUCCESS] MindGlow Quiz Portal is running!
echo Press any key to return to the main menu.
pause >nul
goto menu

:option_fast_run
cls
echo =======================================================================
echo    OPTION [2]: DEPLOY EXISTING WAR, SERVICE BOOT, OPEN BROWSER
echo =======================================================================
echo.
if not exist "target\quizapp.war" (
    echo [WARNING] compiled target\quizapp.war file not found!
    echo We need to perform a full build first.
    echo.
    set "build_choice="
    set /p build_choice="Would you like to build now? (Y/N): "
    if /i "%build_choice%"=="Y" (
        goto option_build_run
    ) else (
        echo Returning to menu...
        ping 127.0.0.1 -n 3 >nul
        goto menu
    )
)
call :sub_check_mysql
call :sub_check_tomcat
call :sub_deploy_war
call :sub_launch_browser
echo.
echo [SUCCESS] MindGlow Quiz Portal is running!
echo Press any key to return to the main menu.
pause >nul
goto menu

:option_stop
cls
echo =======================================================================
echo    OPTION [3]: STOPPING APPLICATION SERVICES
echo =======================================================================
echo.

:: 1. Shutdown Tomcat
echo [1/2] Checking Apache Tomcat status...
netstat -ano | findstr ":8080 " >nul
if not errorlevel 1 (
    echo [SYSTEM] Tomcat is running. Shutting down Tomcat...
    call "C:\tomcat\apache-tomcat-11.0.22\bin\shutdown.bat"
    ping 127.0.0.1 -n 4 >nul
    echo [SUCCESS] Tomcat shutdown signal sent.
) else (
    echo [INFO] Apache Tomcat is already stopped.
)
echo.

:: 2. Shutdown MySQL
echo [2/2] Checking MySQL Server status...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I /N "mysqld.exe" >nul
if not errorlevel 1 (
    echo [SYSTEM] MySQL is running. Gracefully shutting down MySQL...
    "C:\mysql\bin\mysqladmin.exe" -u root shutdown
    ping 127.0.0.1 -n 4 >nul
    
    :: Force kill if still running
    tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I /N "mysqld.exe" >nul
    if not errorlevel 1 (
        echo [WARNING] MySQL did not shutdown gracefully. Force closing...
        taskkill /f /im mysqld.exe >nul
    )
    echo [SUCCESS] MySQL shutdown completed.
) else (
    echo [INFO] MySQL Server is already stopped.
)

echo.
echo =======================================================================
echo [SUCCESS] All application services have been stopped.
echo Press any key to return to the main menu.
pause >nul
goto menu

:option_status
cls
echo =======================================================================
echo    OPTION [4]: APPLICATION SERVICES STATUS
echo =======================================================================
echo.

:: Check MySQL
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I /N "mysqld.exe" >nul
if not errorlevel 1 (
    echo  [*] MySQL Server Status:  [RUNNING] (Process: mysqld.exe)
) else (
    echo  [x] MySQL Server Status:  [STOPPED]
)

:: Check Tomcat
netstat -ano | findstr ":8080 " >nul
if not errorlevel 1 (
    echo  [*] Apache Tomcat Status: [RUNNING] (Listening on port 8080)
) else (
    echo  [x] Apache Tomcat Status: [STOPPED]
)

echo.
echo =======================================================================
echo Press any key to return to the main menu.
pause >nul
goto menu

:option_exit
cls
echo Thank you for using MindGlow Quiz Portal Manager. Goodbye!
ping 127.0.0.1 -n 3 >nul
exit /b


:: =======================================================================
:: SUBROUTINES
:: =======================================================================

:sub_check_mysql
echo [*] Checking MySQL Server status...
tasklist /FI "IMAGENAME eq mysqld.exe" 2>nul | find /I /N "mysqld.exe" >nul
if not errorlevel 1 (
    echo [INFO] MySQL is already running.
) else (
    echo [SYSTEM] MySQL is stopped. Starting MySQL Server...
    start /d "C:\mysql\bin" mysqld.exe
    echo [SYSTEM] Waiting for MySQL to initialize...
    ping 127.0.0.1 -n 5 >nul
    echo [SUCCESS] MySQL Server started successfully.
)
echo.
exit /b

:sub_check_tomcat
echo [*] Checking Apache Tomcat status...
netstat -ano | findstr ":8080 " >nul
if not errorlevel 1 (
    echo [INFO] Apache Tomcat is already running on port 8080.
) else (
    echo [SYSTEM] Apache Tomcat is stopped. Starting Tomcat...
    set "CATALINA_HOME=C:\tomcat\apache-tomcat-11.0.22"
    start "" "C:\tomcat\apache-tomcat-11.0.22\bin\startup.bat"
    echo [SYSTEM] Waiting for Tomcat to initialize...
    ping 127.0.0.1 -n 7 >nul
    echo [SUCCESS] Apache Tomcat started successfully.
)
echo.
exit /b

:sub_build_project
echo [*] Compiling and packaging MindGlow Quiz Portal...
if exist ".maven\apache-maven-3.9.16\bin\mvn" (
    call ".maven\apache-maven-3.9.16\bin\mvn" clean package
) else (
    echo [ERROR] Maven binary not found in expected path: .maven\apache-maven-3.9.16\bin\mvn
    echo Attempting to build with system 'mvn'...
    call mvn clean package
)
if errorlevel 1 (
    echo.
    echo [ERROR] Build failed! Please resolve errors before running.
    pause
    goto menu
)
echo [SUCCESS] Project compiled and packaged successfully.
echo.
exit /b

:sub_deploy_war
echo [*] Deploying WAR file to Tomcat webapps...
copy /y "target\quizapp.war" "C:\tomcat\apache-tomcat-11.0.22\webapps\quizapp.war"
if errorlevel 1 (
    echo [ERROR] Failed to copy WAR file to Tomcat webapps directory.
    echo Please make sure Tomcat path is correct and permissions allow writes.
    pause
    goto menu
)
echo [SUCCESS] quizapp.war deployed to C:\tomcat\apache-tomcat-11.0.22\webapps\
echo.
exit /b

:sub_launch_browser
echo [*] Launching MindGlow Quiz Portal in your default browser...
start http://localhost:8080/quizapp/
echo.
exit /b
