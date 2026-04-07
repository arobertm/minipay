@echo off
echo.
echo ================================
echo  MiniDS - Single Node (Dev Mode)
echo  API:  http://localhost:8301
echo  Raft: localhost:8300
echo ================================
echo.

set NODE_ID=minids-0
set RAFT_PORT=8300
set API_PORT=8301
set RAFT_PEERS=minids-0:8300
set DATA_DIR=./data/minids-0

if not exist "data\minids-0" mkdir "data\minids-0"

java -jar minids/target/minids-1.0.0-SNAPSHOT.jar
