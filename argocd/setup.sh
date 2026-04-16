#!/usr/bin/env bash
# =============================================================================
# Argo CD — Install on k3s + Bootstrap MiniPay Application
# =============================================================================
# Run once on the DigitalOcean droplet as root after k3s is up.
#
# Usage:
#   chmod +x argocd/setup.sh
#   ./argocd/setup.sh
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'; BLUE='\033[0;34m'; NC='\033[0m'
info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }

# ── 1. Install Argo CD ────────────────────────────────────────────────────────
info "Creating argocd namespace..."
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -

info "Installing Argo CD (stable)..."
kubectl apply -n argocd -f \
  https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

info "Waiting for Argo CD server to be ready (this takes ~60s)..."
kubectl rollout status deployment/argocd-server -n argocd --timeout=120s
success "Argo CD installed"

# ── 2. Install Argo CD CLI ────────────────────────────────────────────────────
info "Installing argocd CLI..."
VERSION=$(curl -s https://raw.githubusercontent.com/argoproj/argo-cd/stable/VERSION)
curl -sSL "https://github.com/argoproj/argo-cd/releases/download/v${VERSION}/argocd-linux-amd64" \
  -o /usr/local/bin/argocd
chmod +x /usr/local/bin/argocd
success "argocd CLI installed: $(argocd version --client --short)"

# ── 3. Bootstrap MiniPay Application ─────────────────────────────────────────
info "Applying MiniPay Argo CD Application..."
kubectl apply -f "$(dirname "$0")/application.yaml"
success "Argo CD Application 'minipay' created"

# ── 4. Expose Argo CD UI (NodePort) ──────────────────────────────────────────
info "Exposing Argo CD UI on port 30080..."
kubectl patch svc argocd-server -n argocd \
  --type='json' \
  -p='[{"op":"replace","path":"/spec/type","value":"NodePort"},
       {"op":"add","path":"/spec/ports/0/nodePort","value":30080}]' \
  2>/dev/null || \
kubectl patch svc argocd-server -n argocd \
  -p '{"spec":{"type":"NodePort"}}'

# ── 5. Print summary ──────────────────────────────────────────────────────────
SERVER_IP=$(curl -s http://169.254.169.254/metadata/v1/interfaces/public/0/ipv4/address \
            2>/dev/null || hostname -I | awk '{print $1}')

ARGOCD_PASSWORD=$(kubectl -n argocd get secret argocd-initial-admin-secret \
  -o jsonpath="{.data.password}" | base64 -d)

echo ""
echo -e "${GREEN}============================================${NC}"
echo -e "${GREEN}  Argo CD installed successfully!${NC}"
echo -e "${GREEN}============================================${NC}"
echo ""
echo "  UI:       http://${SERVER_IP}:30080"
echo "  User:     admin"
echo "  Password: ${ARGOCD_PASSWORD}"
echo ""
echo "  After login, change password:"
echo "    argocd login ${SERVER_IP}:30080 --username admin --password '${ARGOCD_PASSWORD}' --insecure"
echo "    argocd account update-password"
echo ""
echo "  Watch sync status:"
echo "    kubectl get applications -n argocd"
echo "    argocd app get minipay"
echo ""
