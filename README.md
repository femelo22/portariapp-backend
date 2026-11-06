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

![Building with AI](https://img.shields.io/badge/Building%20with%20AI-GitHub%20Copilot-000000?logo=githubcopilot&logoColor=white)

O PortariApp é uma API criada para tornar a rotina das portarias mais simples, organizada e segura. A plataforma centraliza o cadastro de moradores, unidades e funcionários, além de acompanhar as encomendas desde o recebimento até a retirada.

Cada movimentação fica registrada em um histórico com data e funcionário responsável, permitindo consultar o status da encomenda e adicionar evidências. Com os dados de cada condomínio separados, o PortariApp oferece mais controle para a equipe, transparência na gestão e tranquilidade para os moradores.

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

## Encomendas

Todas as rotas abaixo exigem autenticação de um funcionário do condomínio. Nas escritas, o
funcionário responsável é obtido do JWT e deve continuar ativo, assim como seu usuário.
O destinatário deve ser um morador ativo do mesmo condomínio.

Base: `/api/v1/condominiums/{condominiumId}/deliveries`

| Método | Caminho | Operação |
|--------|---------|----------|
| POST | `/` | Registrar recebimento (`recipientResidentId`, `description` e `notes`; somente o primeiro é obrigatório). |
| GET | `/` | Listar, com filtros opcionais `status` e `recipientResidentId`, combináveis. |
| GET | `/{deliveryId}` | Consultar o estado atual. |
| GET | `/{deliveryId}/events` | Consultar o histórico em ordem de ocorrência. |
| POST | `/{deliveryId}/pickup` | Registrar retirada (`pickedUpByName` obrigatório e `notes` opcional). |
| POST | `/{deliveryId}/return` | Marcar como devolvida. |
| POST | `/{deliveryId}/lost` | Marcar como extraviada. |
| POST | `/{deliveryId}/cancel` | Cancelar. |
| POST | `/{deliveryId}/evidences` | Enviar imagem no campo multipart `file`. |
| GET | `/{deliveryId}/evidences` | Listar metadados das evidências. |
| GET | `/{deliveryId}/evidences/{evidenceId}/content` | Baixar uma evidência. |

### Regras de negócio

- **RN01 — Recebimento:** o registro de uma encomenda gera um evento `RECEIVED`.
- **RN02 — Situação:** uma encomenda `RECEIVED` pode transitar uma única vez para
  `PICKED_UP`, `RETURNED`, `LOST` ou `CANCELLED`. Essas situações são finais; transições
  repetidas ou inválidas retornam 409.
- **RN03 — Observações:** devolução, extravio e cancelamento aceitam a observação opcional
  `notes`.
- **RN04 — Histórico:** cada evento registra sequência, funcionário, data e observação.
  `deliveries` mantém o estado atual e `delivery_events` mantém o histórico; ambos são
  atualizados na mesma transação, com transições concorrentes serializadas.
- **RN05 — Preservação:** não há rotas para editar ou excluir encomendas ou eventos.
- **RN06 — Evidências:** são opcionais e podem ser adicionadas em qualquer situação.
  Aceitam imagens JPEG, PNG, WebP e HEIC, com limite padrão configurável de 10 MiB por arquivo.

### Armazenamento de evidências

O armazenamento local exige um diretório persistente. A interface permite uma implementação
futura com S3, ainda não incluída. JPEG e PNG são decodificadas para validação; WebP e HEIC
passam apenas por validação estrutural.

Configure `EVIDENCE_STORAGE_DIRECTORY` (padrão `./data/evidences`),
`EVIDENCE_MAX_FILE_SIZE_BYTES` (padrão `10485760`) e `EVIDENCE_MAX_REQUEST_SIZE` (padrão
`11MB`). O limite da requisição inclui os cabeçalhos multipart e deve ser ajustado junto ao
limite do arquivo. Em contêineres, monte um volume persistente no diretório configurado.

Como banco e disco não compartilham uma transação atômica, interrupções podem deixar arquivos
órfãos, que devem ser reconciliados operacionalmente. Se o resultado do commit for desconhecido,
o arquivo é preservado e a ocorrência registrada para reconciliação.


## Autenticação & Autorização

- **Login**: `POST /api/v1/auth/login` (email + senha de um `StaffMember`) retorna um JWT
  (HS256) válido por 8h, com claims `userId`, `staffId` e `condominiumId`. A resposta também
  informa a `role` atual do funcionário; ela não faz parte do JWT.
- **Autenticação**: `JwtAuthenticationFilter` valida o header `Authorization: Bearer <token>` e
  popula o `SecurityContext` com um principal `AuthenticatedStaff`. Sem token válido → 401.
- **Autorização multi-tenant**: `TenantAccessInterceptor` garante que um staff só opere sobre o
  próprio condomínio — qualquer tentativa de acessar outro `condominiumId` (nas rotas aninhadas ou
  no `CondominiumController`) retorna 403. Listar/criar/excluir condomínios é bloqueado para
  staff, por ainda não existir autorização diferenciada para administradores.
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
- [ ] Refresh token / logout — por enquanto só access token com expiração simples; expirou,
      login de novo.
- [ ] Implementar armazenamento de evidências em S3.
