# TA-10 — Environments and Deployment

| Field | Value |
|---|---|
| Document ID | TA-10 |
| Version | 1.0 |
| Status | Approved |
| Owner | Sara Lindqvist, Lead Architect |
| Last updated | 2026-03-16 |
| Related | TA-01, TA-08, TA-09 |

## 1. Hosting

Shelfwise runs on OpenShift 4 on the city's private cloud. City IT provides the platform, the managed database service and the monitoring stack.

## 2. Environments

| Environment | Purpose | Database | Data | Deployment |
|---|---|---|---|---|
| DEV | Development and integration of features | MySQL 8.0, managed | Synthetic | Automatic on every merge to the develop branch |
| TST | Functional testing by the delivery team | MySQL 8.0, managed | Synthetic | Automatic on every merge to main |
| ACC | User acceptance testing and migration rehearsals | MySQL 8.0, managed | Synthetic and masked migration data | Manual, after approval of the product owner |
| PRD | Production | MySQL 8.0, managed, with standby replica | Production | Manual, after approval of the product owner and city IT change board |

## 3. Deployment units

| Unit | Description |
|---|---|
| Backend | Container image based on Eclipse Temurin 21 (JRE), running the Spring Boot application |
| Frontend | Static Angular bundles served by an nginx container |
| Database schema | Flyway migrations, executed at application start-up |

## 4. CI/CD pipeline (GitLab CI)

1. Build and unit tests
2. Integration tests (Testcontainers)
3. Static analysis and dependency scan
4. Container image build and push to the city registry
5. Deployment to DEV or TST (automatic)
6. Deployment to ACC or PRD (manual approval)

During the build phase, a release to ACC is planned every two weeks.

## 5. Monitoring and alerting

- Metrics in Prometheus, dashboards in Grafana, logs in the central logging platform of city IT.
- Alerts for: application unavailable, error rate above 2%, response time targets of TA-08 exceeded, and failure of a nightly batch job.

## 6. Backups

- Managed database backups: daily full backup and continuous archiving of the transaction log (point-in-time recovery), retained for 35 days.
- Backups are encrypted.
