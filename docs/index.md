---
title: Gateway
---

# Gateway

![Java](https://img.shields.io/badge/Java-ED8B00?style=flat&logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-6DB33F?style=flat&logo=springboot&logoColor=white)
![Spring Cloud Gateway](https://img.shields.io/badge/Spring_Cloud_Gateway-6DB33F?style=flat&logo=spring&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-6DB33F?style=flat&logo=springsecurity&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat&logo=docker&logoColor=white)

Gateway is the platform's public API entry point. It routes Auth, Account, Product,
Order and Exchange traffic, enforces browser sessions, JWT/CSRF rules and the
public authorization boundary.

- [Platform documentation](https://microsserv52.github.io/platform/)
- [Gateway service page](https://microsserv52.github.io/platform/services/gateway/)
- [Observability requirements](https://microsserv52.github.io/platform/services/observability-requirements/)
- [Source repository](https://github.com/Microsserv52/gateway)

## Telemetry

| Signal | Endpoint |
| --- | --- |
| Health | `/actuator/health` |
| Metrics | `/actuator/prometheus` |
| Traces | OTLP to Alloy/Tempo via the OpenTelemetry Java agent |
| Logs | Structured JSON to stdout, collected by Alloy/Loki |
