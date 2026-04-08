@echo off
setlocal

set ROOT=c:\disertatie-master
set JAR=%ROOT%\minids\target\minids-1.0.0-SNAPSHOT.jar

:: PEERS foloseste porturile HTTP (API) nu porturile Raft
set PEERS=minids-0:8301,minids-1:8311,minids-2:8321

echo.
echo =========================================
echo  MiniDS - Cluster 3 noduri (Raft demo)
echo  minids-0: http://localhost:8301
echo  minids-1: http://localhost:8311
echo  minids-2: http://localhost:8321
echo =========================================
echo.

if not exist "%JAR%" (
    echo EROARE: Jar-ul nu exista. Ruleaza mai intai:
    echo   mvn package -pl minids -am -DskipTests
    pause
    exit /b 1
)

if not exist "%ROOT%\data\minids-0" mkdir "%ROOT%\data\minids-0"
if not exist "%ROOT%\data\minids-1" mkdir "%ROOT%\data\minids-1"
if not exist "%ROOT%\data\minids-2" mkdir "%ROOT%\data\minids-2"

echo Pornesc minids-0 (port 8301)...
start "minids-0" cmd /k "java -DNODE_ID=minids-0 -DAPI_PORT=8301 -DGRPC_PORT=9081 -DRAFT_PORT=8300 -DRAFT_PEERS=%PEERS% -DRAFT_RESOLVE_LOCALHOST=true -DDATA_DIR=%ROOT%\data\minids-0 -jar %JAR%"

timeout /t 4 /nobreak >nul

echo Pornesc minids-1 (port 8311)...
start "minids-1" cmd /k "java -DNODE_ID=minids-1 -DAPI_PORT=8311 -DGRPC_PORT=9082 -DRAFT_PORT=8310 -DRAFT_PEERS=%PEERS% -DRAFT_RESOLVE_LOCALHOST=true -DDATA_DIR=%ROOT%\data\minids-1 -jar %JAR%"

timeout /t 4 /nobreak >nul

echo Pornesc minids-2 (port 8321)...
start "minids-2" cmd /k "java -DNODE_ID=minids-2 -DAPI_PORT=8321 -DGRPC_PORT=9083 -DRAFT_PORT=8320 -DRAFT_PEERS=%PEERS% -DRAFT_RESOLVE_LOCALHOST=true -DDATA_DIR=%ROOT%\data\minids-2 -jar %JAR%"

echo.
echo Asteapta ~15 secunde si verifica:
echo   curl http://localhost:8301/raft/status
echo   curl http://localhost:8311/raft/status
echo   curl http://localhost:8321/raft/status
echo.
endlocal
