# Repository structure

`banking-platform/` is the Maven aggregator and project root.

| Directory | Maven module | Initial dependency focus |
| --- | --- | --- |
| `account-service/` | Yes | Spring MVC, validation |
| `transaction-service/` | Yes | Spring MVC, validation |
| `api-gateway/` | Yes | Spring Cloud Gateway |
| `discovery-service/` | Yes | Netflix Eureka server |
| `config-server/` | Yes | Spring Cloud Config server |
| `ai-assistant/` | Yes | Spring MVC, validation; AI provider not selected |
| `transaction-consumer/` | Yes | Spring Kafka |
| `fraud-detection/` | Yes | Spring MVC, validation |
| `frontend/` | No | Reserved for the separately selected frontend toolchain |
| `docs/` | No | Architecture and project documentation |

The Maven reactor contains the eight Java service modules. `frontend/` is intentionally not an empty Maven module: its framework and build tool have not been specified. No service contains business logic yet.