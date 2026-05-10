# Git Workflow & CI/CD Pipeline

The FarmConnect project utilizes a professional **GitHub Actions-based CI/CD pipeline** to ensure code quality, build integrity, and automated deployment readiness.

## 🔄 Development Workflow

### 1. Branching Strategy
- **`main`**: Production-ready code. Always stable.
- **Feature Branches (`feat/`)**: Used for new functionality.
- **Fix Branches (`fix/`)**: Used for bug fixes.
- **Workflow**: Create a branch → Develop → Pull Request → CI Check → Merge to `main`.

### 2. Commit Conventions
We follow conventional commits to keep the history readable:
- `fix: resolve logging dependencies`
- `feat: add new reporting dashboard`
- `docs: update service documentation`

---

## 🏗️ CI/CD Pipeline Details

As seen in our **Backend CI/CD Pipeline**, every push to `main` triggers a series of automated jobs:

### 🧪 Stage 1: Quality Gate (Lint & Test)
- Runs `mvn clean test` for all 9 microservices.
- Executes **JQwik** property-based tests to find edge cases.
- Validates code style and security configurations.

### 📦 Stage 2: Containerization (Build & Push)
Once tests pass, the pipeline builds Docker images for each service:
- **Docker Context**: The entire `backend/` directory is used as the build context.
- **Target Registry**: Images are pushed to **GHCR (GitHub Container Registry)**.
- **Versioning**: Images are tagged with the short git commit SHA and `latest`.

### 🚀 Stage 3: Deployment Readiness
- Updated images are ready to be pulled by the production environment (e.g., Kubernetes or Docker Swarm).
- The `docker-compose.yml` in the root of the repo is updated to reflect the latest stable build.

## 📈 Pipeline Status
You can monitor the real-time status of our workflows in the **[Actions Tab](https://github.com/SamppurnaTH/FarmConnect/actions)**.

- ✅ **Green**: Build passed, tests successful, images pushed.
- ❌ **Red**: Pipeline failed. Immediate attention required. Check logs for compilation or test failures.
