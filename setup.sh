#!/usr/bin/env bash
set -euo pipefail

# check required cli tools
for tool in minikube kubectl helm docker; do
    if ! command -v "$tool" &> /dev/null; then
        echo "error: $tool is not installed"
        exit 1
    fi
done

# start minikube cluster with addons
if ! minikube status &> /dev/null; then
    minikube start --cpus=4 --memory=6144 --driver=docker
fi

minikube addons enable ingress
minikube addons enable metrics-server

# create namespaces
for ns in argocd monitoring incident-app; do
    kubectl get namespace "$ns" &> /dev/null || kubectl create namespace "$ns"
done

# install argocd
kubectl apply -n argocd --server-side --force-conflicts -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml
kubectl rollout status deployment/argocd-server -n argocd --timeout=180s || true

# install prometheus and loki charts
helm repo add prometheus-community https://prometheus-community.github.io/helm-charts || true
helm repo add grafana https://grafana.github.io/helm-charts || true
helm repo update

helm upgrade --install prometheus-stack prometheus-community/kube-prometheus-stack \
    --namespace monitoring \
    --set prometheus.prometheusSpec.serviceMonitorSelectorNilUsesHelmValues=false \
    --wait --timeout 300s || true

helm upgrade --install loki-stack grafana/loki-stack \
    --namespace monitoring \
    -f monitoring/loki-stack-values.yaml \
    --wait --timeout 300s || true

# apply argocd application manifest
kubectl apply -f gitops/argocd-application.yaml

echo "setup completed"
echo "argocd port forward: kubectl port-forward svc/argocd-server -n argocd 8080:443"
echo "argocd initial password: kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d && echo"
echo "grafana port forward: kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3000:80"
echo "check application pods: kubectl get pods -n incident-app"
