#!/bin/bash
# Start MiniDS 3-node cluster from bash (for Windows/Git Bash)
# Usage: bash scripts/start-minids-bash.sh

ROOT="c:/disertatie-master"
JAR="$ROOT/minids/target/minids-1.0.0-SNAPSHOT.jar"
PEERS="minids-0:8301,minids-1:8311,minids-2:8321"

if [ ! -f "$JAR" ]; then
  echo "ERROR: JAR not found: $JAR"
  echo "Run: mvn package -pl minids -am -DskipTests"
  exit 1
fi

# Kill any existing nodes
for PORT in 8301 8311 8321; do
  PID=$(netstat -ano 2>/dev/null | grep ":$PORT " | grep LISTEN | awk '{print $5}' | head -1)
  if [ -n "$PID" ]; then
    echo "Killing PID $PID on port $PORT"
    taskkill /PID "$PID" /F 2>/dev/null
    sleep 1
  fi
done

echo "Starting minids-0 (port 8301)..."
java -DNODE_ID=minids-0 -DAPI_PORT=8301 -DGRPC_PORT=9081 -DRAFT_PORT=8300 \
  "-DRAFT_PEERS=$PEERS" -DRAFT_RESOLVE_LOCALHOST=true \
  "-DDATA_DIR=$ROOT/data/minids-0" \
  -jar "$JAR" > /tmp/minids0.log 2>&1 &

sleep 3

echo "Starting minids-1 (port 8311)..."
java -DNODE_ID=minids-1 -DAPI_PORT=8311 -DGRPC_PORT=9082 -DRAFT_PORT=8310 \
  "-DRAFT_PEERS=$PEERS" -DRAFT_RESOLVE_LOCALHOST=true \
  "-DDATA_DIR=$ROOT/data/minids-1" \
  -jar "$JAR" > /tmp/minids1.log 2>&1 &

sleep 3

echo "Starting minids-2 (port 8321)..."
java -DNODE_ID=minids-2 -DAPI_PORT=8321 -DGRPC_PORT=9083 -DRAFT_PORT=8320 \
  "-DRAFT_PEERS=$PEERS" -DRAFT_RESOLVE_LOCALHOST=true \
  "-DDATA_DIR=$ROOT/data/minids-2" \
  -jar "$JAR" > /tmp/minids2.log 2>&1 &

echo "All 3 nodes starting. Waiting 20s for leader election..."
sleep 20

echo ""
echo "=== Raft Status ==="
curl -s http://localhost:8301/raft/status && echo ""
curl -s http://localhost:8311/raft/status && echo ""
curl -s http://localhost:8321/raft/status && echo ""
