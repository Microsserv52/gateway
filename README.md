# Gateway

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

| Variable | Purpose |
| --- | --- |
| `JWT_SECRET` | Shared JWT HMAC secret |
| `AUTH_URL`, `ACCOUNT_URL`, `ORDER_URL` | Platform service URLs |
| `PRODUCT_URL`, `EXCHANGE_URL` | External service URLs |

## GitHub Actions secrets

Add these under **Settings → Secrets and variables → Actions** in
`Microsserv52/gateway`:

- `JWT_SECRET`: shared JWT HMAC secret.
- `AUTH_URL`, `ACCOUNT_URL`, `ORDER_URL`, `PRODUCT_URL`, `EXCHANGE_URL`:
  internal base URLs for the five upstream services.
