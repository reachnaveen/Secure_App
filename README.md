## 1. Project Overview

This repository contains a secure full-stack application featuring a Spring Boot backend and a React frontend. It is designed to demonstrate a complete CI/CD pipeline using GitHub Actions for automated building, testing, and security scanning, coupled with a GitOps workflow for deployments to Kubernetes via Argo CD.

## 2. Getting Started: Local Development Setup

Follow these instructions to get the application running on your local machine for development and testing purposes.

### 2.1. Prerequisites

Make sure you have the following software installed on your system:

*   **Java Development Kit (JDK)**: Version 17 or later.
*   **Maven**: Version 3.6 or later (for building the backend).
*   **Node.js**: Version 18 or later.
*   **npm**: Version 8 or later (comes with Node.js).
*   **Docker**: For building and running containerized versions of the applications.

### 2.2. Configuration

The backend uses Google for OAuth2 authentication. You will need to create OAuth 2.0 credentials in the Google Cloud Console.

1.  Go to the [Google Cloud Console Credentials page](https://console.cloud.google.com/apis/credentials).
2.  Create a new **OAuth 2.0 Client ID**.
3.  Select **Web application** as the application type.
4.  Add `http://localhost:8080` to **Authorized JavaScript origins**.
5.  Add `http://localhost:8080/login/oauth2/code/google` to **Authorized redirect URIs**.
6.  Once created, note your **Client ID** and **Client Secret**.

You must provide these credentials to the Spring Boot application. The recommended way is to set them as environment variables:

```bash
export GOOGLE_CLIENT_ID="your-client-id-from-google"
export GOOGLE_CLIENT_SECRET="your-client-secret-from-google"
```

### 2.3. Running the Backend

1.  Navigate to the backend directory: `cd backend`
2.  Run the application using the Maven wrapper. It will automatically pick up the environment variables you set.
    ```bash
    ./mvnw spring-boot:run
    ```
3.  The backend API will be running at `http://localhost:8080`.

### 2.4. Running the Frontend

1.  In a new terminal, navigate to the frontend directory: `cd ui`
2.  Install the required npm packages:
    ```bash
    npm install
    ```
3.  Start the frontend development server:
    ```bash
    npm start
    ```
4.  The React application will be accessible at `http://localhost:3000`. It is configured to proxy API requests to the backend server running on port 8080.

---

## 3. GitHub Actions Workflow

A GitHub Actions workflow (`.github/workflows/ci-cd.yml`) is provided to automate the build, test, security scanning, Docker image creation/push, and GitOps deployment triggering for your application.

### 3.1 Workflow Structure

*   **`on`**: Configured to trigger on `push` events to `main`, `develop`, and `feature/*` branches, and also via `workflow_dispatch` for manual runs.
*   **`env`**: Defines common environment variables like Docker registry and image names.
*   **`jobs`**:
    *   **`build-and-test`**: This job performs the core CI tasks:
        *   **Checkout Code**: Fetches the repository content.
        *   **Setup JDK & Node.js**: Installs the necessary runtime environments.
        *   **Docker Login**: Authenticates with Docker Hub (or your configured registry) using GitHub Secrets.
        *   **Image Tagging**: Generates unique image tags based on commit SHA and timestamp.
        *   **Backend Build, Test & Scan**: Runs Maven commands for building, testing (unit, integration, REST Assured, WireMock, Pact provider), SAST (SpotBugs), and SCA (OWASP Dependency-Check). Passes OAuth secrets as build arguments and environment variables.
        *   **Build and Push Backend Docker Image**: Builds the backend Docker image and pushes it to the registry.
        *   **Frontend Build, Test & Scan**: Installs npm dependencies, runs frontend tests, and performs SCA (`npm audit`).
        *   **Build and Push Frontend Docker Image**: Builds the frontend Docker image and pushes it to the registry.
        *   **Outputs**: Exports the generated backend and frontend image tags for subsequent deployment jobs.

    *   **`deploy-dev`**: This job handles automated deployment to the `dev` environment.
        *   **`needs`**: Depends on `build-and-test` to ensure images are built and pushed.
        *   **`environment`**: Configured with a `dev` environment for tracking deployments in GitHub.
        *   **Checkout GitOps Repo**: Checks out the repository (or a dedicated GitOps repository).
        *   **Update Kustomize Image Tag**: Uses `sed` to update the image tags for both backend and frontend in `k8s/overlays/dev/deployment.yaml`.
        *   **Commit and Push Dev Changes**: Commits the updated `deployment.yaml` back to the repository. This commit acts as the trigger for Argo CD (or Flux) to deploy the new images to the `dev` Kubernetes cluster.

    *   **`deploy-stage`**: This job handles manual promotion to the `stage` environment.
        *   **`needs`**: Depends on `build-and-test`.
        *   **`environment`**: Configured with a `stage` environment and a URL for tracking.
        *   **`if` condition**: Only runs when the workflow is manually dispatched and the `environment` input is set to `stage`.
        *   **Checkout GitOps Repo**: Checks out the repository.
        *   **Update Kustomize Image Tag**: Updates the image tags in `k8s/overlays/stage/deployment.yaml`.
        *   **Commit and Push Stage Changes**: Commits the updated `deployment.yaml` to trigger Argo CD for the `stage` environment.

    *   **`deploy-prod`**: This job handles manual promotion to the `prod` environment.
        *   **`needs`**: Depends on `build-and-test`.
        *   **`environment`**: Configured with a `prod` environment and a URL for tracking.
        *   **`if` condition**: Only runs when the workflow is manually dispatched and the `environment` input is set to `prod`.
        *   **Checkout GitOps Repo**: Checks out the repository.
        *   **Update Kustomize Image Tag**: Updates the image tags in `k8s/overlays/prod/deployment.yaml`.
        *   **Commit and Push Prod Changes**: Commits the updated `deployment.yaml` to trigger Argo CD for the `prod` environment.

### 3.2 GitHub Secrets Configuration

For the GitHub Actions workflow to function correctly, you must configure the following secrets in your GitHub repository settings (`Settings > Secrets and variables > Actions > New repository secret`):

*   `DOCKER_USERNAME`: Your Docker Hub username (or the username for your private container registry).
*   `DOCKER_PASSWORD`: Your Docker Hub Personal Access Token (PAT) or the password for your private container registry. **It is highly recommended to use a PAT instead of your main password.**
*   `GOOGLE_CLIENT_ID`: The client ID for your Google OAuth 2.0 application.
*   `GOOGLE_CLIENT_SECRET`: The client secret for your Google OAuth 2.0 application.
*   `GIT_OPS_REPO_PAT`: A Personal Access Token (PAT) for a Git user that has **write access to this repository**. This PAT is used by the GitHub Actions workflow to commit the updated Kustomize files back to the repository, which is essential for the GitOps flow. Ensure this PAT has `repo` scope.

### 3.3 Frontend Dockerfile

For the frontend Docker build step in the pipeline to work, you need a `Dockerfile` in your `ui/` directory. A basic example is provided in the `ui/Dockerfile` file within this repository.

## 4. GitHub Flow and GitOps Integration

This project leverages the GitHub Flow branching strategy, which integrates seamlessly with the GitHub Actions workflow and the GitOps deployment model.

### 4.1 GitHub Flow Explained

*   **`main` Branch**: This branch always represents the **production-ready** code. Only stable, tested, and reviewed code is merged here. Pushes to `main` trigger the GitHub Actions workflow to build, test, and push images, and then allow for manual promotion to `stage` and `prod` environments.

*   **`develop` Branch (Recommended)**: This branch serves as the **integration branch** for ongoing development. Feature branches are merged into `develop`. Pushes to `develop` trigger the GitHub Actions workflow to build, test, push images, and automatically deploy to the `dev` environment, providing a quick feedback loop for integrated changes.

*   **Feature Branches**: For every new feature, bug fix, or enhancement, a new branch is created off `develop` (e.g., `feature/add-new-product`, `bugfix/fix-login-issue`). Developers work on their changes in isolation within these branches.

*   **Pull Requests (PRs)**: When a feature is complete, a Pull Request is opened from the feature branch to `develop` (or `main` for releases). This is the core of the collaboration and quality gate:
    *   **CI/CD Trigger**: Opening or updating a PR triggers the `build-and-test` job of the GitHub Actions workflow, running all automated checks (builds, tests, SAST, SCA, Docker image builds/pushes).
    *   **Code Review**: Peers review the code changes.
    *   **Automated Quality Gates**: The pipeline results (test passes, scan reports) are visible directly in the PR, ensuring that only high-quality code is merged.
    *   **Pact Verification**: The backend's Pact provider verification test runs in CI, ensuring that the backend's API remains compatible with the frontend's expectations.

*   **Merging to `develop`**: Once a PR is approved and all CI checks pass, the feature branch is merged into `develop`. This merge triggers the GitHub Actions workflow to build, test, push images, and automatically update the `dev` environment's Kustomize configuration, which Argo CD then deploys.

*   **Releasing to `stage` and `prod`**: When `develop` is stable and ready for a release, a Pull Request is opened from `develop` to `main`. Upon approval and successful CI, merging to `main` triggers the GitHub Actions workflow to build, test, and push images. Manual `workflow_dispatch` events are then used to trigger deployments to `stage` and `prod` environments, updating their respective Kustomize configurations for Argo CD to deploy.

### 4.2 GitOps in the Context of GitHub Flow

*   **Source of Truth**: Your Git repository (specifically the `main` and `develop` branches) acts as the single source of truth for the desired state of your applications and infrastructure.

*   **Automated Reconciliation**: GitOps operators (like Argo CD or Flux, deployed in your Kubernetes cluster) continuously monitor the Git repository. When a change is detected (e.g., a new Docker image tag in a Kustomize overlay or Helm `values.yaml` file), they automatically pull and apply those changes to the Kubernetes cluster.

*   **Deployment as a Git Commit**: Instead of the CI pipeline directly executing `kubectl apply` commands, it commits changes (e.g., updating image tags) to the GitOps repository. This approach offers significant benefits:
    *   **Audit Trail**: Every deployment is a Git commit, providing a clear, immutable history.
    *   **Easy Rollbacks**: To roll back a deployment, simply revert the corresponding Git commit.
    *   **Collaboration**: All changes to the infrastructure are reviewed and approved via standard Git PR workflows.
    *   **Security**: Reduces the need for CI agents to have direct `kubectl` access to production clusters.

This integrated approach ensures a robust, automated, and secure development and deployment pipeline, aligning with modern DevOps principles.

## 5. Observability: Tracing, Metrics, and Logging

To gain deep insights into the application's behavior, performance, and health in a production environment, it's crucial to implement a robust observability strategy. This typically consists of three pillars: distributed tracing, metrics, and centralized logging.

### 5.1. Distributed Tracing with OpenTelemetry

Distributed tracing allows you to follow a single request as it travels through different services in your application (e.g., from the frontend to the backend to a database). OpenTelemetry is the industry standard for instrumenting your code to generate trace data.

**Backend (Spring Boot):**

The easiest way to enable tracing for a Java application is by using the OpenTelemetry Java agent. This agent can be attached to your application without any code changes and will automatically instrument popular frameworks and libraries (like Spring MVC, JDBC, etc.).

To integrate it into your Kubernetes deployment, you would:
1.  Download the OpenTelemetry Java agent JAR.
2.  Add it to your backend's Docker image or make it available via an `initContainer` in Kubernetes.
3.  Update your Kubernetes `Deployment` manifest to attach the agent using the `JAVA_TOOL_OPTIONS` environment variable.

Here is an example snippet for your `k8s/base/deployment.yaml` or an environment-specific overlay:

```yaml
# In your backend container spec
spec:
  containers:
  - name: secure-app-backend
    # ... other container properties
    env:
    - name: JAVA_TOOL_OPTIONS
      value: "-javaagent:/path/to/opentelemetry-javaagent.jar"
    - name: OTEL_SERVICE_NAME
      value: "secure-app-backend"
    - name: OTEL_EXPORTER_OTLP_ENDPOINT
      value: "http://otel-collector.observability.svc.cluster.local:4317" # URL of your OpenTelemetry Collector
```

This configuration will send trace data to an OpenTelemetry Collector, which can then forward it to a tracing backend like Jaeger, Zipkin, or Grafana Tempo.

### 5.2. Metrics with Prometheus

Prometheus is a leading open-source monitoring system that collects and stores its metrics as time-series data.

**Backend (Spring Boot):**

Spring Boot Actuator, combined with the Micrometer Prometheus registry, makes exposing Prometheus-compatible metrics trivial.

1.  **Add Dependencies**: Ensure your `pom.xml` includes:
    ```xml
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>
    ```
2.  **Configure Actuator**: In your `application.properties` or `application.yml`, expose the Prometheus endpoint:
    ```properties
    management.endpoints.web.exposure.include=health,info,prometheus
    management.endpoint.prometheus.enabled=true
    ```
This will create a `/actuator/prometheus` endpoint on your backend service that Prometheus can scrape. In Kubernetes, you would typically create a `ServiceMonitor` custom resource to configure Prometheus to automatically discover and scrape this endpoint.

### 5.3. Centralized Logging

In a containerized environment, applications should log to standard output (`stdout`) and standard error (`stderr`). A cluster-level logging agent is then responsible for collecting, parsing, and forwarding these logs to a centralized storage backend.

**Best Practices:**
*   **Structured Logging**: Configure your application to output logs in a structured format like JSON. This makes them much easier to parse, search, and analyze. For Spring Boot, you can use libraries like Logstash Logback Encoder.
*   **Log Collection**: Deploy a log collection agent like Fluentd, Promtail, or Vector as a `DaemonSet` in your Kubernetes cluster.
*   **Log Storage & Querying**: Store logs in a system like Elasticsearch or Grafana Loki. Loki is often preferred in cloud-native environments for its efficiency and integration with Prometheus and Grafana.

### 5.4. Visualization with Grafana

Grafana is a powerful open-source platform for querying, visualizing, and alerting on metrics, logs, and traces. It can connect to various data sources, allowing you to create a unified observability dashboard.

*   **Metrics**: Connect Grafana to your Prometheus instance to build dashboards for JVM metrics, HTTP request rates, error counts, and latency.
*   **Logs**: Connect Grafana to Loki or Elasticsearch to search and visualize logs.
*   **Traces**: Connect Grafana to Jaeger or Tempo to visualize distributed traces and correlate them with logs and metrics.

By implementing these tools, you create a comprehensive observability stack that provides the visibility needed to operate your application reliably and efficiently.

### 5.5. Local Observability Stack with Docker Compose

To facilitate local development and testing, you can run a complete observability stack (Prometheus, Grafana, Loki) on your machine using Docker Compose. This allows you to inspect metrics and logs from your application as you develop.

#### Setup

1.  Create a new directory named `observability` in the root of your project.
2.  Inside the `observability` directory, create the following four files and one directory:
    *   `docker-compose.yml`
    *   `prometheus.yml`
    *   `loki-config.yml`
    *   `promtail-config.yml`
    *   A directory named `logs`

**1. `docker-compose.yml`**

This file defines the services for Prometheus, Loki, Promtail (the log collector for Loki), and Grafana.

```yaml
# observability/docker-compose.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    container_name: prometheus
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
    networks:
      - monitoring

  loki:
    image: grafana/loki:latest
    container_name: loki
    ports:
      - "3100:3100"
    volumes:
      - ./loki-config.yml:/etc/loki/local-config.yaml
    command:
      - '-config.file=/etc/loki/local-config.yaml'
    networks:
      - monitoring

  promtail:
    image: grafana/promtail:latest
    container_name: promtail
    volumes:
      - ./logs:/applogs
      - ./promtail-config.yml:/etc/promtail/config.yml
    command:
      - '-config.file=/etc/promtail/config.yml'
    networks:
      - monitoring

  grafana:
    image: grafana/grafana:latest
    container_name: grafana
    ports:
      - "3000:3000"
    volumes:
      - grafana-data:/var/lib/grafana
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    networks:
      - monitoring

networks:
  monitoring:
    driver: bridge

volumes:
  grafana-data:
```

**2. `prometheus.yml`**

This configures Prometheus to scrape the metrics endpoint of your locally running Spring Boot application.

```yaml
# observability/prometheus.yml
global:
  scrape_interval: 15s

scrape_configs:
  - job_name: 'secure-app-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      # Use host.docker.internal to allow the Prometheus container to reach
      # the application running on the host machine.
      - targets: ['host.docker.internal:8080']
```
> **Note:** `host.docker.internal` is available by default on Docker Desktop for Mac and Windows. For Linux, you may need to add `extra_hosts: ["host.docker.internal:host-gateway"]` to the `prometheus` service in your `docker-compose.yml`.

**3. `loki-config.yml`**

A basic configuration file for Loki.

```yaml
# observability/loki-config.yml
auth_enabled: false
server:
  http_listen_port: 3100
ingester:
  lifecycler:
    address: 127.0.0.1
    ring:
      kvstore:
        store: inmemory
      replication_factor: 1
    final_sleep: 0s
  chunk_idle_period: 1m
schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h
storage_config:
  boltdb_shipper:
    active_index_directory: /tmp/loki/boltdb-shipper-active
    shared_store: filesystem
  filesystem:
    directory: /tmp/loki/chunks
```

**4. `promtail-config.yml`**

This configures Promtail to read log files from the `logs` directory you created and send them to Loki.

```yaml
# observability/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
- job_name: backend-logs
  static_configs:
  - targets:
      - localhost
    labels:
      job: secure-app-backend
      __path__: /applogs/backend.log
```

#### Running the Stack

1.  **Start the observability stack**:
    ```bash
    cd observability
    docker-compose up -d
    ```
2.  **Run your backend and pipe logs**: In a separate terminal, run your Spring Boot application and redirect its standard output to the log file that Promtail is watching.
    ```bash
    # From the project root directory
    ./backend/mvnw -f ./backend/pom.xml spring-boot:run > ./observability/logs/backend.log 2>&1
    ```

#### Accessing and Configuring Grafana

1.  **Open Grafana**: Navigate to `http://localhost:3000` in your browser. Log in with username `admin` and password `admin`.
2.  **Add Prometheus Data Source**:
    *   Go to `Connections > Data sources > Add new data source`.
    *   Select **Prometheus**.
    *   Set the **Prometheus server URL** to `http://prometheus:9090`.
    *   Click **Save & Test**.
3.  **Add Loki Data Source**:
    *   Go to `Connections > Data sources > Add new data source`.
    *   Select **Loki**.
    *   Set the **Loki server URL** to `http://loki:3100`.
    *   Click **Save & Test**.

You can now go to the **Explore** view in Grafana to query your application's metrics from Prometheus and its logs from Loki.

#### Importing the Pre-configured Dashboard

To get you started quickly, a pre-configured Grafana dashboard is included in this repository.

1.  The file `observability/grafana-dashboard.json` contains a basic dashboard for monitoring key Spring Boot application metrics.
2.  In the Grafana UI (`http://localhost:3000`), navigate to the **Dashboards** section in the left-hand menu.
3.  Click on **Import** in the top right corner.
4.  Click **Upload JSON file** and select the `observability/grafana-dashboard.json` file from your project directory.
5.  On the next screen, under **Options**, make sure to select your `Prometheus` data source from the dropdown.
6.  Click **Import**. You will be taken to your new dashboard, which should start displaying metrics from your running backend application.

## 6. Kubernetes Deployment Best Practices

To ensure your application runs reliably and handles deployments gracefully in Kubernetes, it's essential to configure health checks.

### 6.1. Health Checks (Liveness & Readiness Probes)

Kubernetes uses probes to determine the health of your application's containers. This allows the cluster to manage your application's lifecycle automatically, for instance by restarting a crashed container or by temporarily disabling traffic to a container that is not yet ready.

*   **Readiness Probes**: These tell Kubernetes when your application is ready to start accepting traffic. If a readiness probe fails, the container's IP is removed from the Service's endpoints, effectively taking it out of the load balancer's rotation until it becomes ready again. This is crucial for zero-downtime deployments.
*   **Liveness Probes**: These tell Kubernetes if your application is still running or has entered a broken state (e.g., a deadlock). If a liveness probe fails, Kubernetes will kill the container and restart it according to its `restartPolicy`.

This project's base Kubernetes deployment has been updated to include these probes, leveraging the specific health endpoints provided by Spring Boot Actuator.

#### Spring Boot Configuration

To enable the dedicated `liveness` and `readiness` health endpoints, you must add the following property to your backend's `application.properties` or `application.yml`:

```properties
# application.properties
management.endpoint.health.probes.enabled=true
```

This creates two new endpoints that the probes will use:
*   `/actuator/health/readiness`: The application is considered "ready" when it can service traffic.
*   `/actuator/health/liveness`: The application is considered "live" as long as it's running and not in a broken internal state.

#### Kubernetes Deployment Configuration

The configuration in `k8s/base/deployment.yaml` uses these endpoints to give Kubernetes fine-grained control over the application's lifecycle, leading to higher availability and smoother deployments. The `initialDelaySeconds` values are set to give the application adequate time to start up before the probes become active.

### 6.2. Resource Requests and Limits

Specifying CPU and memory requests and limits for your containers is essential for cluster stability and application performance.

*   **Requests**: This is the amount of resources that Kubernetes guarantees for your container. The Kubernetes scheduler uses this value to find a suitable node for your pod.
*   **Limits**: This is the maximum amount of resources your container can consume. A container that exceeds its CPU limit will be throttled, and one that exceeds its memory limit will be terminated (OOMKilled).

Setting both requests and limits places your pod in the `Burstable` Quality of Service (QoS) class, which is a good default for most web applications. It guarantees a baseline level of resources while allowing the application to "burst" and use more resources if they are available on the node.

#### Kubernetes Deployment Configuration

The `k8s/base/deployment.yaml` file has been updated with sensible starting values.

```yaml
resources:
  requests:
    cpu: "250m"
    memory: "512Mi"
  limits:
    cpu: "1"
    memory: "1Gi"
```

You should monitor your application's actual resource consumption in Grafana and adjust these values to match its real-world performance profile.

## 7. ArgoCD Configuration

This project uses ArgoCD for continuous delivery, following GitOps principles. The `argocd/` directory contains the ArgoCD Application manifests for each environment.

*   `argocd/dev-app.yaml`: Deploys the application to the `dev` namespace, tracking the `k8s/overlays/dev` directory.
*   `argocd/stage-app.yaml`: Deploys the application to the `stage` namespace, tracking the `k8s/overlays/stage` directory.
*   `argocd/prod-app.yaml`: Deploys the application to the `prod` namespace, tracking the `k8s/overlays/prod` directory.

To use these, apply them to your cluster where ArgoCD is running:

```bash
kubectl apply -f argocd/dev-app.yaml
kubectl apply -f argocd/stage-app.yaml
kubectl apply -f argocd/prod-app.yaml
```

**Note:** You must replace the `repoURL` placeholder in each file with the actual URL of your Git repository.
