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

## GitHub Actions secrets

Add these under **Settings → Secrets and variables → Actions** in
`Microsserv52/gateway`:

- `JWT_SECRET`: shared JWT HMAC secret.
- `AUTH_URL`, `ACCOUNT_URL`, `ORDER_URL`, `PRODUCT_URL`, `EXCHANGE_URL`:
  internal base URLs for the five upstream services.
