# PortariApp

[![Java](https://img.shields.io/badge/Java-21-007396?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-JWT-6DB33F?logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Flyway](https://img.shields.io/badge/Flyway-Migrations-CC0200?logo=flyway&logoColor=white)](https://flywaydb.org/)
[![Docker](https://img.shields.io/badge/Docker%20Compose-local%20stack-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-observability-425CC7?logo=opentelemetry&logoColor=white)](https://opentelemetry.io/)
[![Prometheus](https://img.shields.io/badge/Prometheus-metrics-E6522C?logo=prometheus&logoColor=white)](https://prometheus.io/)
[![Grafana](https://img.shields.io/badge/Grafana-dashboards-F46800?logo=grafana&logoColor=white)](https://grafana.com/)
[![Loki](https://img.shields.io/badge/Loki-logs-F5A623?logo=grafana&logoColor=white)](https://grafana.com/oss/loki/)
[![Swagger](https://img.shields.io/badge/OpenAPI-Swagger%20UI-85EA2D?logo=swagger&logoColor=black)](https://swagger.io/)

API para gestão de portaria de condomínios: controle de moradores, unidades, porteiros/síndicos
e recebimento/entrega de encomendas, com isolamento multi-tenant por condomínio.

## Stack

| Camada           | Tecnologia |
|------------------|------------|
| Linguagem        | Java 21 |
| Framework        | Spring Boot 4.1 (Web MVC, Validation, Data JPA, Actuator, Security) |
| Banco de dados   | PostgreSQL + Flyway (versionamento de schema) |
| Autenticação     | JWT (io.jsonwebtoken), stateless |
| Observabilidade  | OpenTelemetry (métricas/logs/tracing) via OTLP |
| Documentação API | SpringDoc OpenAPI (Swagger UI) |
| Infra local      | Docker Compose (Postgres, OTel Collector, Prometheus, Loki, Grafana) |

## Arquitetura

Camadas clássicas com contratos explícitos, visando testabilidade e baixo acoplamento:

```
Controller (interface)  →  define o contrato HTTP (rota, verbo, validação de entrada)
ControllerImpl           →  implementação fina, delega para o Service
Service (interface)      →  regra de negócio
ServiceImpl               →  implementação, orquestra Repository + Mapper
Repository (Spring Data) →  acesso a dados
Mapper                   →  Entity ↔ DTO (métodos estáticos, sem framework)
```

Recursos de condomínio (residents, units, staff-members) são aninhados sob
`/api/v1/condominiums/{condominiumId}/...`, reforçando o isolamento por tenant já na própria URL.

## Autenticação & Autorização

- **Login**: `POST /api/v1/auth/login` (email + senha de um `StaffMember`) retorna um JWT
  (HS256) válido por 8h, com claims `userId`, `staffId` e `condominiumId`.
- **Autenticação**: `JwtAuthenticationFilter` valida o header `Authorization: Bearer <token>` e
  popula o `SecurityContext` com um principal `AuthenticatedStaff`. Sem token válido → 401.
- **Autorização multi-tenant**: `TenantAccessInterceptor` garante que um staff só opere sobre o
  próprio condomínio — qualquer tentativa de acessar outro `condominiumId` (nas rotas aninhadas ou
  no `CondominiumController`) retorna 403. Listar/criar/excluir condomínios é bloqueado para
  staff, por ainda não existir um papel de admin.
- Senhas com **BCrypt** (`spring-security-crypto`).

## Tratamento de exceções

`GlobalExceptionHandler` centraliza a conversão de exceções em respostas HTTP, sempre no mesmo
formato (`ErrorResponse`: timestamp, status, error, message, path, fieldErrors):

| Exceção                     | HTTP |
|-----------------------------|------|
| `ResourceNotFoundException` | 404  |
| `ConflictException`         | 409  |
| `BadRequestException`       | 400  |
| `UnauthorizedException`     | 401  |
| `ForbiddenException`        | 403  |
| `MethodArgumentNotValidException` (Bean Validation) | 400 (com `fieldErrors`) |
| `DataIntegrityViolationException` | 409  |

## Observabilidade

Perfil `dev` liga exportação OTLP de métricas e logs para a stack local do `docker-compose.yml`:

- **OpenTelemetry Collector** recebe métricas/logs/traces da aplicação
- **Prometheus** faz scrape das métricas do collector
- **Loki** armazena os logs estruturados
- **Grafana** (dashboards provisionados em `observability/grafana`) unifica a visualização

## Rodando localmente

```bash
# sobe Postgres + stack de observabilidade
docker compose up -d

# roda a aplicação (perfil dev ativa OTLP)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Swagger UI: `http://localhost:8080/swagger-ui.html`
Grafana: `http://localhost:3000`

## TODO

- [ ] Criar uma descrição do projeto
- [X] Criar README do projeto
- [X] Autorização multi-tenant
- [ ] Transições de status adicionais do delivery — RETURNED, LOST, CANCELLED (hoje só
      implementamos RECEIVED → PICKED_UP).
- [ ] Filtros na listagem de deliveries — por status e/ou morador (recipientResidentId); por ora
      é lista simples ordenada por receivedAt.
- [ ] Refresh token / logout — por enquanto só access token com expiração simples; expirou,
      login de novo.
- [ ] Storage de evidências em S3 — a interface EvidenceStorageService já fica pronta para isso,
      mas a implementação (S3EvidenceStorageService) não é criada agora, só o ponto de extensão.
