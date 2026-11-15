# incident management platform

incident management platform with spring boot backend, react frontend, postgresql, and gitops deployment via argocd and minikube.

## prerequisites

make sure the following tools are installed:
- [docker](https://www.docker.com/) (must be running)
- [minikube](https://minikube.sigs.k8s.io/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [helm](https://helm.sh/)

## quick start (minikube)

1. start docker desktop or your local docker daemon.
2. run the setup script to bootstrap the cluster, monitoring stack, argocd, and the application:

```bash
chmod +x setup.sh
./setup.sh
```

3. verify that all pods are running:

```bash
kubectl get pods -n incident-app
```

## access the application

### option 1: port forward (simplest)

forward the frontend service to your localhost:

```bash
kubectl port-forward svc/app-chart-frontend -n incident-app 3000:80
```

open your browser at:
`http://localhost:3000`

### option 2: ingress with minikube tunnel

1. in a separate terminal window, start the tunnel:

```bash
minikube tunnel
```

2. map the local domain in `/etc/hosts`:

```bash
echo "127.0.0.1 incident-platform.local" | sudo tee -a /etc/hosts
```

3. open your browser at:
`http://incident-platform.local`

## access devops and monitoring tools

### argocd dashboard

1. port forward the argocd server:

```bash
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

2. retrieve the initial admin password:

```bash
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath='{.data.password}' | base64 -d && echo
```

3. open `https://localhost:8080` (username: `admin`).

### grafana (metrics and logs)

1. port forward grafana:

```bash
kubectl port-forward svc/prometheus-stack-grafana -n monitoring 3001:80
```

2. open `http://localhost:3001` (username: `admin`, password: `prom-operator`).

## metrics and exporters

every source is scraped by prometheus through a `ServiceMonitor` from `charts/app-chart`:

| source | what | how |
|---|---|---|
| backend | http traffic and latency (histogram), jvm, hikari pool | spring boot actuator + micrometer, `/actuator/prometheus` |
| postgres_exporter | database server: `pg_up`, connections, size, locks, transactions | sidecar in the postgres pod, connects over localhost |
| sql_exporter | business metrics: incidents by status/severity, oldest open incident, unassigned incidents, users | queries in `charts/app-chart/files/incidents.collector.yml` |

logs don't need an exporter: promtail ships pod stdout to loki, the backend logs json (`{namespace="incident-app"} | json | level="WARN"`).

### why these exporters

- **postgres_exporter** - the standard exporter for postgres metrics, maintained by prometheus-community, ready-made grafana dashboards exist for it
  - its custom queries feature (`extend.query-path`) is deprecated, so it can't be used for the business metrics
- **sql_exporter** - turns any sql query into a metric through config only, so business metrics need no code to build, test and ship
  - replaced a custom python exporter that ran the same queries
  - alternative considered: micrometer gauges in the backend - no extra pod, but every backend replica would report the same database-wide numbers
- every status/severity combination is always reported (also with 0), so panels and alerts show 0 instead of "no data"

## dashboards and alerts

everything ships with `charts/app-chart`, so argocd deploys monitoring together with the app:

```
backend / exporters ──> prometheus ──> PrometheusRule ──> alertmanager ──> alert-logger pod ──> loki ──> grafana
                            └──────────────> grafana dashboards (ConfigMaps)
```

### dashboards

`charts/app-chart/dashboards/*.json`, one ConfigMap each with label `grafana_dashboard: "1"`, the grafana sidecar from kube-prometheus-stack loads them (folder "General", tag `incident-platform`):

| dashboard | panels |
|---|---|
| Incident Platform / Backend | pods up, request rate, 5xx ratio, p95 latency, firing alerts; traffic and latency per endpoint; heap, cpu, gc, threads, hikari pool, restarts; warn/error logs and alert notifications from loki |
| Incident Platform / Incidents | unresolved/critical/unassigned incidents, oldest critical incident, users; current state and trends by status and severity |
| Incident Platform / PostgreSQL | up, size, connection usage, cache hit ratio, tps, longest transaction; connections, transactions, row ops, locks |

### alerts

`templates/prometheusrule.yaml`, thresholds in `values.yaml` under `prometheusRule.thresholds`:

| alert | severity | fires when |
|---|---|---|
| BackendDown | critical | no backend pod scrapeable for 1m |
| BackendPodDown | warning | one backend pod not scrapeable for 3m |
| PostgresDown | critical | `pg_up` not 1 (db unreachable or db pod gone) for 1m |
| ExporterDown | warning | postgres_exporter or sql_exporter has no healthy target for 5m |
| HighErrorRate | critical | >5% of api requests return 5xx for 5m |
| HighLatencyP95 | warning | p95 api latency >1s for 10m |
| CriticalIncidentOpenTooLong | critical | oldest unresolved CRITICAL incident older than 1h |
| UnassignedIncidentsPilingUp | warning | >10 unresolved incidents without assignee for 15m |

- api rate, 5xx ratio and p95 are recording rules (`job:http_server_requests:*`), actuator endpoints excluded
- `AlertmanagerConfig` routes alerts from this namespace to a webhook receiver: `alert-logger` (http echo server) logs each notification as json, so firing/resolved alerts show up in the backend dashboard via loki
- inhibit rules keep only the root cause: `BackendDown` mutes `BackendPodDown`, `PostgresDown` mutes `ExporterDown` for postgres_exporter
- down alerts use `absent(... == 1)` so they also fire when the target disappears; `absent()` drops labels, so these rules set `namespace` explicitly for the route

```bash
# alertmanager ui
kubectl port-forward svc/prometheus-stack-kube-prom-alertmanager -n monitoring 9093:9093
# notifications received by the logger
kubectl logs -n incident-app deploy/incident-management-platform-app-chart-alert-logger
```

## stopping minikube

when you are done, you can stop the local cluster:

```bash
minikube stop
```
