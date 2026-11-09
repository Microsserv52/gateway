# Gateway

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat)](https://dev.java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![Spring Cloud](https://img.shields.io/badge/Spring_Cloud-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-gateway)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)](https://github.com/features/actions)

Spring Cloud Gateway MVC public entry point. It validates JWTs and routes
Auth, Account, Order, Product, and Exchange requests. Nginx or a cloud load
balancer can proxy to the gateway; multiple gateway replicas are supported.

## Run locally

```bash
./mvnw test
./mvnw verify
./mvnw spring-boot:run
```

## Environment variables

Use the root platform `.env` for local URLs and credentials. Configure the
same names as repository secrets for Actions, and use ConfigMap values plus
`platform-secrets` for Kubernetes.

| Variable                               | Purpose                                                        |
| -------------------------------------- | -------------------------------------------------------------- |
| `JWT_SECRET`                           | Shared JWT HMAC secret                                         |
| `AUTH_URL`, `ACCOUNT_URL`, `ORDER_URL` | Platform service URLs                                          |
| `PRODUCT_URL`, `EXCHANGE_URL`          | External service URLs                                          |
| `SESSION_COOKIE_SECURE`                | Defaults to `true`; local HTTP Compose explicitly uses `false` |
| `SESSION_COOKIE_PATH`                  | Defaults to `/api`, the browser-facing API prefix              |

## Browser sessions and authorization

Static frontends call these routes through the same-origin `/api` proxy. The
Gateway itself receives paths without that prefix:

| Route                 | Behavior                                                                                                                   |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------- |
| `GET /session/csrf`   | Issues a CSRF cookie and returns the masked token and header name                                                          |
| `POST /session/login` | Requires the CSRF cookie/header; exchanges credentials with Auth, returns 204 and an HttpOnly, SameSite=Lax session cookie |
| `GET /session`        | Validates the cookie JWT, reads `/accounts/me`, and returns the account profile                                            |
| `DELETE /session`     | Clears the session cookie; requires CSRF when a session cookie is present                                                  |

No JWT is returned in the browser login response. Explicit Bearer-token API
clients remain supported. Browser mutations obtain a fresh CSRF token before
submitting. Use TLS and secure cookies outside local HTTP testing.

Catalog GETs are public. Product mutations and account administration require
`ROLE_ADMIN`; `/accounts/me` requires authentication. The public Gateway denies
`/accounts/internal/**`. New account registration remains public. Product's own
authorization and network isolation must also be verified before deployment.

`./mvnw verify` covers public reads, role checks, CSRF, the Auth exchange and
profile lookup. The workstation currently has JDK 24, so local evidence uses
`./mvnw -Djava.version=24 verify`; the repository's normal build target stays 25.

## GitHub Actions secrets

Add these under **Settings → Secrets and variables → Actions** in
`Microsserv52/gateway`:

- `JWT_SECRET`: shared JWT HMAC secret.
- `AUTH_URL`, `ACCOUNT_URL`, `ORDER_URL`, `PRODUCT_URL`, `EXCHANGE_URL`:
  internal base URLs for the five upstream services.
