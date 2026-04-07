@echo off
setlocal

:: Calea absoluta catre radacina proiectului
set ROOT=c:\disertatie-master
set JAR=%ROOT%\minids\target\minids-1.0.0-SNAPSHOT.jar
set PEERS=minids-0:8300,minids-1:8310,minids-2:8320
set RESOLVE=true

echo.
echo =========================================
echo  MiniDS - Cluster 3 noduri (Raft demo)
echo  minids-0: http://localhost:8301
echo  minids-1: http://localhost:8311
echo  minids-2: http://localhost:8321
echo =========================================
echo.

:: Verifica daca jar-ul exista
if not exist "%JAR%" (
    echo EROARE: Jar-ul nu exista la %JAR%
    echo Ruleaza mai intai: mvn package -pl minids -am -DskipTests
    pause
    exit /b 1
)

:: Creeaza directoarele de date
if not exist "%ROOT%\data\minids-0" mkdir "%ROOT%\data\minids-0"
if not exist "%ROOT%\data\minids-1" mkdir "%ROOT%\data\minids-1"
if not exist "%ROOT%\data\minids-2" mkdir "%ROOT%\data\minids-2"

echo Pornesc minids-0 (API:8301, Raft:8300, gRPC:9081)...
start "minids-0" cmd /k "set NODE_ID=minids-0& set RAFT_PORT=8300& set API_PORT=8301& set GRPC_PORT=9081& set RAFT_PEERS=%PEERS%& set RAFT_RESOLVE_LOCALHOST=%RESOLVE%& set DATA_DIR=%ROOT%\data\minids-0& java -jar %JAR%"

timeout /t 3 /nobreak >nul

echo Pornesc minids-1 (API:8311, Raft:8310, gRPC:9082)...
start "minids-1" cmd /k "set NODE_ID=minids-1& set RAFT_PORT=8310& set API_PORT=8311& set GRPC_PORT=9082& set RAFT_PEERS=%PEERS%& set RAFT_RESOLVE_LOCALHOST=%RESOLVE%& set DATA_DIR=%ROOT%\data\minids-1& java -jar %JAR%"

timeout /t 3 /nobreak >nul

echo Pornesc minids-2 (API:8321, Raft:8320, gRPC:9083)...
start "minids-2" cmd /k "set NODE_ID=minids-2& set RAFT_PORT=8320& set API_PORT=8321& set GRPC_PORT=9083& set RAFT_PEERS=%PEERS%& set RAFT_RESOLVE_LOCALHOST=%RESOLVE%& set DATA_DIR=%ROOT%\data\minids-2& java -jar %JAR%"

echo.
echo Asteapta ~15 secunde pentru leader election...
echo.
echo Verifica status dupa pornire:
echo   curl http://localhost:8301/raft/status
echo   curl http://localhost:8311/raft/status
echo   curl http://localhost:8321/raft/status
echo.
endlocal
