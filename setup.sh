#!/usr/bin/env bash
# Creates a local minikube cluster with monitoring, ArgoCD and the incident platform.
# Start from scratch with: minikube delete && ./setup.sh
set -euo pipefail

ARGOCD_VERSION=v3.6.1
KUBE_PROMETHEUS_STACK_VERSION=93.4.0
LOKI_STACK_VERSION=2.10.3

for tool in minikube kubectl helm docker openssl; do
    if ! command -v "$tool" > /dev/null; then
        echo "error: $tool is not installed"
        exit 1
    fi
done

if ! docker info > /dev/null 2>&1; then
    echo "error: docker is not running"
    exit 1
fi

echo "==> minikube"
# Calico: the default minikube network plugin does not enforce NetworkPolicy
minikube start --driver=docker --cpus=4 --memory=6144 --cni=calico
minikube addons enable ingress
# metrics-server: needed by the HorizontalPodAutoscaler and kubectl top
minikube addons enable metrics-server

echo "==> monitoring (Prometheus, Grafana, Loki)"
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts
helm repo add grafana https://grafana.github.io/helm-charts
helm repo update

helm upgrade --install prometheus-stack prometheus-community/kube-prometheus-stack \
    --version "$KUBE_PROMETHEUS_STACK_VERSION" \
    --namespace monitoring --create-namespace \
    -f monitoring/kube-prometheus-stack-values.yaml \
    --wait --timeout 10m

helm upgrade --install loki-stack grafana/loki-stack \
    --version "$LOKI_STACK_VERSION" \
    --namespace monitoring \
    -f monitoring/loki-stack-values.yaml \
    --wait --timeout 5m

echo "==> ArgoCD"
kubectl create namespace argocd --dry-run=client -o yaml | kubectl apply -f -
# --server-side: the ArgoCD CRDs are too big for a client-side apply
kubectl apply -n argocd --server-side --force-conflicts \
    -f "https://raw.githubusercontent.com/argoproj/argo-cd/${ARGOCD_VERSION}/manifests/install.yaml"
kubectl rollout status deployment/argocd-server -n argocd --timeout=5m

echo "==> application namespace and database secret"
kubectl apply -f gitops/namespace.yaml
# the password is generated here and never stored in git
if ! kubectl get secret postgres-credentials -n incident-app > /dev/null 2>&1; then
    kubectl create secret generic postgres-credentials -n incident-app \
        --type=kubernetes.io/basic-auth \
        --from-literal=username=platform \
        --from-literal=password="$(openssl rand -hex 16)"
fi

echo "==> ArgoCD application"
kubectl apply -f gitops/argocd-application.yaml

cat <<'EOF'

Setup finished. ArgoCD is now deploying the application, watch it with:
  kubectl get pods -n incident-app -w

ArgoCD UI:   kubectl port-forward svc/argocd-server -n argocd 8080:443
             password: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
Grafana:     kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3001:80   (admin / prom-operator)
Application: minikube tunnel   (separate terminal), then http://incident-platform.local
EOF
