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

## stopping minikube

when you are done, you can stop the local cluster:

```bash
minikube stop
```
