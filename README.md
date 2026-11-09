# Gateway

**Application stack**

[![Java](https://img.shields.io/badge/Java-ED8B00?style=flat)](https://dev.java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Bean Validation](https://img.shields.io/badge/Bean_Validation-6DB33F?style=flat)](https://jakarta.ee/specifications/bean-validation/)
[![Lombok](https://img.shields.io/badge/Lombok-BC4521?style=flat)](https://projectlombok.org/)
[![Spring MVC](https://img.shields.io/badge/Spring_MVC-6DB33F?style=flat&logo=spring&logoColor=white)](https://docs.spring.io/spring-framework/reference/web/webmvc.html)
[![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![OAuth 2.0](https://img.shields.io/badge/OAuth_2.0-3C6E71?style=flat)](https://oauth.net/2/)
[![JWT](https://img.shields.io/badge/JWT-000000?style=flat&logo=jsonwebtokens&logoColor=white)](https://jwt.io/)
[![Spring Actuator](https://img.shields.io/badge/Spring_Actuator-6DB33F?style=flat&logo=springboot&logoColor=white)](https://docs.spring.io/spring-boot/reference/actuator/index.html)
[![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=flat&logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud-gateway)

**Build, checks and delivery**

[![Maven](https://img.shields.io/badge/Maven-C71A36?style=flat&logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![JUnit](https://img.shields.io/badge/JUnit-25A162?style=flat&logo=junit5&logoColor=white)](https://junit.org/)
[![Git](https://img.shields.io/badge/Git-F05032?style=flat&logo=git&logoColor=white)](https://git-scm.com/)
[![GitHub Actions](https://img.shields.io/badge/GitHub_Actions-2088FF?style=flat&logo=githubactions&logoColor=white)](https://github.com/features/actions)
[![CodeQL](https://img.shields.io/badge/CodeQL-181717?style=flat&logo=github&logoColor=white)](https://codeql.github.com/)
[![JaCoCo](https://img.shields.io/badge/JaCoCo-D24939?style=flat)](https://www.jacoco.org/jacoco/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)](https://www.docker.com/)
[![GHCR](https://img.shields.io/badge/GHCR-181717?style=flat&logo=github&logoColor=white)](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

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
