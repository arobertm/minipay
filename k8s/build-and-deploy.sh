#!/usr/bin/env bash
# =============================================================================
# MiniPay — Build & Deploy to k3s on DigitalOcean
# =============================================================================
# Run this script on the DigitalOcean droplet as root (or with sudo).
#
# Usage:
#   chmod +x k8s/build-and-deploy.sh
#   ./k8s/build-and-deploy.sh [REPO_URL]
#
# Example:
#   ./k8s/build-and-deploy.sh https://github.com/YOUR_USER/disertatie-master.git
#
# What this script does:
#   1. Installs Docker + Java 21 + Maven 3.9 (if not present)
#   2. Clones or updates the repo
#   3. Builds all JARs with Maven
#   4. Builds Docker images
#   5. Imports images into k3s containerd
#   6. Applies all Kubernetes manifests
# =============================================================================

set -euo pipefail

REPO_URL="${1:-}"
REPO_DIR="/opt/minipay"
NAMESPACE="minipay"

# ── Colors ────────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'; BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERROR]${NC} $*"; exit 1; }

# ── 1. Install dependencies ───────────────────────────────────────────────────
install_docker() {
    if command -v docker &>/dev/null; then
        success "Docker already installed: $(docker --version)"
        return
    fi
    info "Installing Docker..."
    curl -fsSL https://get.docker.com | sh
    systemctl enable --now docker
    success "Docker installed"
}

install_java() {
    if java -version 2>&1 | grep -q "21\|22\|23"; then
        success "Java 21+ already installed"
        return
    fi
    info "Installing OpenJDK 21..."
    apt-get update -qq
    apt-get install -y --no-install-recommends openjdk-21-jdk-headless
    success "Java installed: $(java -version 2>&1 | head -1)"
}

install_maven() {
    if command -v mvn &>/dev/null; then
        success "Maven already installed: $(mvn --version | head -1)"
        return
    fi
    info "Installing Maven 3.9..."
    local MVN_VERSION="3.9.6"
    local MVN_URL="https://archive.apache.org/dist/maven/maven-3/${MVN_VERSION}/binaries/apache-maven-${MVN_VERSION}-bin.tar.gz"
    curl -fsSL "$MVN_URL" -o /tmp/maven.tar.gz
    tar -xzf /tmp/maven.tar.gz -C /opt
    ln -sf "/opt/apache-maven-${MVN_VERSION}/bin/mvn" /usr/local/bin/mvn
    rm /tmp/maven.tar.gz
    success "Maven installed: $(mvn --version | head -1)"
}

info "=== Step 1: Installing dependencies ==="
install_docker
install_java
install_maven

# ── 2. Clone or update repository ─────────────────────────────────────────────
info "=== Step 2: Cloning/updating repository ==="
if [[ -z "$REPO_URL" ]]; then
    if [[ -d "$REPO_DIR/.git" ]]; then
        warn "No REPO_URL provided; using existing repo at $REPO_DIR"
        cd "$REPO_DIR"
        git pull
    else
        error "No REPO_URL provided and no repo at $REPO_DIR. Usage: $0 <git-repo-url>"
    fi
else
    if [[ -d "$REPO_DIR/.git" ]]; then
        info "Repo already cloned — pulling latest..."
        cd "$REPO_DIR" && git pull
    else
        info "Cloning $REPO_URL → $REPO_DIR"
        git clone "$REPO_URL" "$REPO_DIR"
        cd "$REPO_DIR"
    fi
fi
success "Repository ready at $REPO_DIR"

# ── 3. Build JARs with Maven ──────────────────────────────────────────────────
info "=== Step 3: Building JARs (this takes a few minutes) ==="
cd "$REPO_DIR"
mvn clean package -DskipTests \
    --batch-mode \
    -q \
    2>&1 | tail -20
success "Maven build complete"

# ── 4. Build Docker images ────────────────────────────────────────────────────
info "=== Step 4: Building Docker images ==="

build_image() {
    local name="$1"
    local dockerfile="$2"
    info "  Building minipay/${name}:latest ..."
    docker build \
        --build-arg BUILDKIT_INLINE_CACHE=1 \
        -t "minipay/${name}:latest" \
        -f "$dockerfile" \
        . \
        -q
    success "  minipay/${name}:latest built"
}

cd "$REPO_DIR"

# MiniDS — multi-stage build (Maven inside Docker, no pre-built JAR needed)
build_image "minids"       "minids/Dockerfile"

# Java microservices — copy pre-built JARs from target/
build_image "auth-svc"     "services/auth-svc/Dockerfile"
build_image "user-svc"     "services/user-svc/Dockerfile"
build_image "session-svc"  "services/session-svc/Dockerfile"
build_image "gateway-svc"  "services/gateway-svc/Dockerfile"
build_image "vault-svc"    "services/vault-svc/Dockerfile"
build_image "network-svc"  "services/network-svc/Dockerfile"
build_image "issuer-svc"   "services/issuer-svc/Dockerfile"
build_image "audit-svc"    "services/audit-svc/Dockerfile"

# Python service
build_image "fraud-svc"    "services/fraud-svc/Dockerfile"

success "All Docker images built"

# ── 5. Import images into k3s containerd ─────────────────────────────────────
info "=== Step 5: Importing images into k3s ==="

import_image() {
    local name="$1"
    info "  Importing minipay/${name}:latest → k3s..."
    docker save "minipay/${name}:latest" | k3s ctr images import -
    success "  minipay/${name}:latest imported"
}

import_image "minids"
import_image "auth-svc"
import_image "user-svc"
import_image "session-svc"
import_image "gateway-svc"
import_image "vault-svc"
import_image "network-svc"
import_image "issuer-svc"
import_image "audit-svc"
import_image "fraud-svc"

success "All images imported into k3s"

# ── 6. Apply Kubernetes manifests ─────────────────────────────────────────────
info "=== Step 6: Applying Kubernetes manifests ==="

K8S_DIR="$REPO_DIR/k8s"

# Apply in dependency order
kubectl apply -f "$K8S_DIR/namespace.yaml"
kubectl apply -f "$K8S_DIR/secrets.yaml"

# Infrastructure first
kubectl apply -f "$K8S_DIR/infra/"
info "  Waiting for PostgreSQL to be ready..."
kubectl rollout status statefulset/postgres -n "$NAMESPACE" --timeout=120s
info "  Waiting for Kafka to be ready..."
kubectl rollout status statefulset/kafka -n "$NAMESPACE" --timeout=180s

# MiniDS Raft cluster
kubectl apply -f "$K8S_DIR/data/"
info "  Waiting for MiniDS cluster (3 pods, Raft quorum)..."
kubectl rollout status statefulset/minids -n "$NAMESPACE" --timeout=180s

# Identity services
kubectl apply -f "$K8S_DIR/identity/"
info "  Waiting for auth-svc..."
kubectl rollout status deployment/auth-svc -n "$NAMESPACE" --timeout=120s

# Payments + security
kubectl apply -f "$K8S_DIR/payments/"
kubectl apply -f "$K8S_DIR/security/"

# Monitoring
kubectl apply -f "$K8S_DIR/monitoring/"

# Ingress
kubectl apply -f "$K8S_DIR/ingress.yaml"

success "All manifests applied"

# ── 7. Summary ────────────────────────────────────────────────────────────────
echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}  MiniPay deployed successfully!${NC}"
echo -e "${GREEN}========================================${NC}"

SERVER_IP=$(curl -s http://169.254.169.254/metadata/v1/interfaces/public/0/ipv4/address 2>/dev/null || hostname -I | awk '{print $1}')

echo ""
echo "  Server IP: $SERVER_IP"
echo ""
echo "  Endpoints:"
echo "    Payment API  → http://${SERVER_IP}/api/"
echo "    OAuth2/OIDC  → http://${SERVER_IP}/auth/.well-known/oauth-authorization-server"
echo "    Users API    → http://${SERVER_IP}/users/"
echo "    MiniDS       → http://${SERVER_IP}/minids/v1/health"
echo "    Grafana      → http://${SERVER_IP}/grafana/  (admin/admin)"
echo "    Kafka UI     → http://${SERVER_IP}/kafka/"
echo ""
echo "  Check pod status:"
echo "    kubectl get pods -n minipay"
echo ""
