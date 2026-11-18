# Incident Management Platform

Spring Boot backend, React frontend and PostgreSQL, deployed to Kubernetes (minikube) with a Helm chart,
GitHub Actions and ArgoCD (GitOps).

## Repository

| Path | Content |
|---|---|
| `app/platform` | backend - Java 25, Spring Boot 4, Flyway, Spock tests |
| `app/frontend` | frontend - React 19, TypeScript, Vite, served by nginx |
| `charts/incident-platform` | Helm chart with all Kubernetes manifests, deployed by ArgoCD |
| `gitops` | namespace and the ArgoCD `Application` |
| `monitoring` | values for kube-prometheus-stack and loki-stack |
| `.github/workflows/ci-cd.yml` | CI/CD pipeline |
| `setup.sh` | creates the whole local environment |

## Running locally

Requirements: Docker (running), minikube, kubectl, helm.

```bash
minikube delete   # optional - start from a clean cluster
./setup.sh
kubectl get pods -n incident-app -w
```

`setup.sh` starts minikube with Calico (needed for NetworkPolicy), installs Prometheus/Grafana, Loki and ArgoCD,
creates the namespace and the database Secret (random password, not stored in git) and registers the ArgoCD
application. ArgoCD then deploys `charts/incident-platform`.

### Application

```bash
minikube tunnel                                                    # separate terminal
echo "127.0.0.1 incident-platform.local" | sudo tee -a /etc/hosts  # once
```

Open http://incident-platform.local. Without the tunnel: `kubectl port-forward svc/frontend -n incident-app 3000:80`
and open http://localhost:3000.

### ArgoCD

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d
```

Open https://localhost:8080, user `admin`.

### Grafana

```bash
kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3001:80
```

Open http://localhost:3001 (`admin` / `prom-operator`). Dashboards: *Incident Platform / Backend, Incidents, PostgreSQL*.

## CI/CD

```
pull request ──> tests, lint, CodeQL, Trivy (dependencies)
merge to main ─> the same + build images ──> push to GHCR ──> Trivy (images)
               ──> commit new image tag to charts/incident-platform/values.yaml
               ──> ArgoCD notices the commit and runs a rolling update
```

ArgoCD checks the repository every 3 minutes. To check right away: *Refresh* in the ArgoCD UI.

## Kubernetes objects

| Object | Where |
|---|---|
| Deployment | backend, frontend, sql-exporter |
| StatefulSet + headless Service | postgres (with postgres-exporter sidecar) |
| Service | backend, frontend, postgres, sql-exporter |
| Ingress | `/` -> frontend, `/api` -> backend |
| NetworkPolicy | default deny for incoming traffic + allow rules per component |
| PersistentVolumeClaim | database (`volumeClaimTemplates`), backups (`postgres-backup`) |
| ConfigMap | backend config, sql-exporter queries, Grafana dashboards |
| Secret | `postgres-credentials`, created by `setup.sh` |
| Requests/limits, ResourceQuota, LimitRange | `values.yaml`, `resourcequota.yaml`, `limitrange.yaml` |
| HorizontalPodAutoscaler | backend, 2-3 replicas by CPU |
| CronJob | nightly `pg_dump` backup |
| ServiceAccount | backend, without an API token |
| ServiceMonitor, PrometheusRule | metrics scraping and alerts |
