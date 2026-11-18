# Commands

Useful commands for running, checking and testing the platform. All application resources live in the
`incident-app` namespace.

## Setup

```bash
minikube delete                  # optional - start from a clean cluster
./setup.sh

# once - map the Ingress host to localhost (asks for your Mac password)
echo "127.0.0.1 incident-platform.local" | sudo tee -a /etc/hosts
```

## Access

```bash
# Ingress - keep this terminal open, then use http://incident-platform.local
minikube tunnel

# without Ingress
kubectl port-forward svc/frontend -n incident-app 3000:80          # http://localhost:3000
kubectl port-forward svc/backend -n incident-app 8080:8080         # http://localhost:8080/api/incidents

# ArgoCD - https://localhost:8443 (login: see Passwords)
kubectl port-forward svc/argocd-server -n argocd 8443:443

# Grafana - http://localhost:3001 (login: see Passwords)
kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3001:80

# Prometheus - http://localhost:9090 (Status -> Targets, Alerts)
kubectl port-forward svc/prometheus-stack-kube-prom-prometheus -n monitoring 9090:9090

# Alertmanager - http://localhost:9093
kubectl port-forward svc/prometheus-stack-kube-prom-alertmanager -n monitoring 9093:9093
```

## Passwords

All passwords are generated when the cluster is created and kept in Kubernetes Secrets - none of them is in git.
They change after every `minikube delete && ./setup.sh`.

```bash
# ArgoCD - user: admin
kubectl get secret argocd-initial-admin-secret -n argocd -o jsonpath='{.data.password}' | base64 -d; echo

# Grafana - user: admin
kubectl get secret prometheus-stack-grafana -n monitoring -o jsonpath='{.data.admin-user}' | base64 -d; echo
kubectl get secret prometheus-stack-grafana -n monitoring -o jsonpath='{.data.admin-password}' | base64 -d; echo

# PostgreSQL - user and password of the application database (created by setup.sh)
kubectl get secret postgres-credentials -n incident-app -o jsonpath='{.data.username}' | base64 -d; echo
kubectl get secret postgres-credentials -n incident-app -o jsonpath='{.data.password}' | base64 -d; echo

# PostgreSQL - open psql inside the database pod (no password needed, local connection)
kubectl exec -it postgres-0 -n incident-app -c postgres -- psql -U platform -d incidents
#   \dt                          list tables
#   select * from incidents;
#   \q                           quit
```

Prometheus, Alertmanager and Loki have no login - they are only reachable through `kubectl port-forward`,
not from outside the cluster.

## Cluster

```bash
kubectl get nodes -o wide
kubectl get pods -n kube-system | grep -E "calico|coredns"
minikube addons list | grep -E "ingress|metrics-server"
kubectl get pods -A
```

## Deployment (ArgoCD)

```bash
kubectl get application incident-management-platform -n argocd     # Synced / Healthy
kubectl get all -n incident-app
kubectl get pods -n incident-app -o wide
kubectl get pods -n incident-app -w                                 # watch, Ctrl+C to stop
```

## Kubernetes objects

```bash
# Deployment / StatefulSet
kubectl get deploy,sts -n incident-app
kubectl describe deploy backend -n incident-app

# Service (postgres is headless: CLUSTER-IP None)
kubectl get svc,endpoints -n incident-app

# Ingress and Ingress Controller
kubectl get pods -n ingress-nginx
kubectl get ingressclass
kubectl describe ingress incident-platform -n incident-app

# NetworkPolicy
kubectl get networkpolicy -n incident-app
kubectl describe networkpolicy postgres -n incident-app

# PersistentVolumeClaim
kubectl get pvc,pv -n incident-app
kubectl get storageclass

# ConfigMap
kubectl get configmap -n incident-app
kubectl describe configmap backend-config -n incident-app

# Secret (shows keys and sizes, not values)
kubectl describe secret postgres-credentials -n incident-app

# Requests / limits, ResourceQuota, LimitRange
kubectl describe resourcequota -n incident-app
kubectl describe limitrange -n incident-app
kubectl top pods -n incident-app

# Additional objects
kubectl get hpa,cronjob,serviceaccount -n incident-app
kubectl get namespace incident-app --show-labels                    # Pod Security Admission labels
kubectl get servicemonitor,prometheusrule -n incident-app

# Images running in the cluster (tag set by CI)
kubectl get pods -n incident-app -o custom-columns='POD:.metadata.name,IMAGE:.spec.containers[*].image'
```

> zsh: always put arguments containing `[*]` in quotes, otherwise you get `no matches found`.

## Functional tests (REST API)

Run with `minikube tunnel`. Without it, use `kubectl port-forward svc/backend -n incident-app 8080:8080`
and replace `http://incident-platform.local` with `http://localhost:8080`.

```bash
API=http://incident-platform.local/api

# list
curl -s $API/incidents
curl -s "$API/incidents?status=OPEN"
curl -s $API/users

# incident lifecycle: 201 + Location, status OPEN, then RESOLVED with resolvedAt
curl -i -X POST $API/incidents -H "Content-Type: application/json" \
  -d '{"title": "Login page down", "severity": "HIGH", "assigneeId": 2}'
curl -s $API/incidents/<id>
curl -s -X PUT $API/incidents/<id> -H "Content-Type: application/json" \
  -d '{"title": "Login page down", "severity": "HIGH", "status": "RESOLVED", "assigneeId": 2}'

# 409 Conflict - user 1 has an IN_PROGRESS incident (seed data)
curl -i -X DELETE $API/users/1

# 400 Bad Request - assignee does not exist
curl -i -X POST $API/incidents -H "Content-Type: application/json" \
  -d '{"title": "Test", "severity": "LOW", "assigneeId": 999}'
```

## Infrastructure tests

```bash
# NetworkPolicy - a pod without an allowed label can't reach the database (timeout = blocked)
kubectl run np-test -n incident-app --rm -it --image=busybox -- nc -zv -w 3 postgres 5432

# Persistence - data survives a database restart
kubectl delete pod postgres-0 -n incident-app
kubectl get pods -n incident-app -w
curl -s http://incident-platform.local/api/incidents

# Deployment recreates deleted pods
kubectl delete pod -l app.kubernetes.io/name=backend -n incident-app --wait=false
kubectl get pods -n incident-app -w

# ArgoCD self-heal - manual change is reverted to what is in git
kubectl scale deploy frontend -n incident-app --replicas=3
kubectl get deploy frontend -n incident-app -w

# CronJob - run the backup now
kubectl create job manual-backup --from=cronjob/postgres-backup -n incident-app
kubectl logs job/manual-backup -n incident-app
kubectl delete job manual-backup -n incident-app

# HPA
kubectl get hpa backend -n incident-app
kubectl describe hpa backend -n incident-app
```

## Observability

```bash
# backend logs (JSON, Logbook masks the email field as "XXX")
kubectl logs -l app.kubernetes.io/name=backend -n incident-app --tail=50

# business metrics from sql-exporter
kubectl port-forward svc/sql-exporter -n incident-app 9399:9399
curl -s localhost:9399/metrics | grep incident_platform

# backend metrics
curl -s http://localhost:8080/actuator/prometheus | grep http_server_requests   # needs port-forward to backend
curl -s http://localhost:8080/actuator/health
```

Prometheus queries: `pg_up`, `incident_platform_incidents`, `up{namespace="incident-app"}`, `ALERTS`.

## CI/CD and GitOps

```bash
# after a merge to main: ArgoCD picks up the new image tag (checks git every 3 minutes, or Refresh in the UI)
kubectl get application incident-management-platform -n argocd
kubectl rollout history deploy/backend -n incident-app
kubectl rollout history deploy/frontend -n incident-app
kubectl get pods -n incident-app -o custom-columns='POD:.metadata.name,IMAGE:.spec.containers[*].image'
git log --oneline -5 origin/main                                    # "deploy: images sha-..." commit by the bot
```

## Troubleshooting

```bash
kubectl get pods -A | grep -v Running
kubectl describe pod <pod> -n incident-app
kubectl logs <pod> -n incident-app --all-containers
kubectl logs <pod> -n incident-app --previous                       # logs of the crashed container
kubectl get events -n incident-app --sort-by=.lastTimestamp
```

## Stop / remove

```bash
minikube stop       # keeps the cluster, start again with: minikube start
minikube delete     # removes everything, next time run ./setup.sh
```
