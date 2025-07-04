## 5. GitHub Actions Workflow

A GitHub Actions workflow (`.github/workflows/ci-cd.yml`) is provided to automate the build, test, security scanning, Docker image creation/push, and GitOps deployment triggering for your application.

### 5.1 Workflow Structure

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

### 5.2 GitHub Secrets Configuration

For the GitHub Actions workflow to function correctly, you must configure the following secrets in your GitHub repository settings (`Settings > Secrets and variables > Actions > New repository secret`):

*   `DOCKER_USERNAME`: Your Docker Hub username (or the username for your private container registry).
*   `DOCKER_PASSWORD`: Your Docker Hub Personal Access Token (PAT) or the password for your private container registry. **It is highly recommended to use a PAT instead of your main password.**
*   `GOOGLE_CLIENT_ID`: The client ID for your Google OAuth 2.0 application.
*   `GOOGLE_CLIENT_SECRET`: The client secret for your Google OAuth 2.0 application.
*   `GIT_OPS_REPO_PAT`: A Personal Access Token (PAT) for a Git user that has **write access to this repository**. This PAT is used by the GitHub Actions workflow to commit the updated Kustomize files back to the repository, which is essential for the GitOps flow. Ensure this PAT has `repo` scope.

### 5.3 Frontend Dockerfile

For the frontend Docker build step in the pipeline to work, you need a `Dockerfile` in your `frontend/` directory. A basic example is provided in the `frontend/Dockerfile` file within this repository.

## 6. GitHub Flow and GitOps Integration

This project leverages the GitHub Flow branching strategy, which integrates seamlessly with the GitHub Actions workflow and the GitOps deployment model.

### 6.1 GitHub Flow Explained

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

### 6.2 GitOps in the Context of GitHub Flow

*   **Source of Truth**: Your Git repository (specifically the `main` and `develop` branches) acts as the single source of truth for the desired state of your applications and infrastructure.

*   **Automated Reconciliation**: GitOps operators (like Argo CD or Flux, deployed in your Kubernetes cluster) continuously monitor the Git repository. When a change is detected (e.g., a new Docker image tag in a Kustomize overlay or Helm `values.yaml` file), they automatically pull and apply those changes to the Kubernetes cluster.

*   **Deployment as a Git Commit**: Instead of the CI pipeline directly executing `kubectl apply` commands, it commits changes (e.g., updating image tags) to the GitOps repository. This approach offers significant benefits:
    *   **Audit Trail**: Every deployment is a Git commit, providing a clear, immutable history.
    *   **Easy Rollbacks**: To roll back a deployment, simply revert the corresponding Git commit.
    *   **Collaboration**: All changes to the infrastructure are reviewed and approved via standard Git PR workflows.
    *   **Security**: Reduces the need for CI agents to have direct `kubectl` access to production clusters.

This integrated approach ensures a robust, automated, and secure development and deployment pipeline, aligning with modern DevOps principles.
