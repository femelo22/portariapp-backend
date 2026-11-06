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

O recebimento cria um evento `RECEIVED`. A partir dele, há somente uma transição para
`PICKED_UP`, `RETURNED`, `LOST` ou `CANCELLED`; estados finais não podem ser alterados.
Os três últimos endpoints aceitam um corpo opcional `{"notes": "..."}`. Tentativas de
transição repetida ou inválida retornam 409. A tabela `deliveries` guarda o estado atual e
`delivery_events` registra o histórico, com sequência, funcionário, data e observação.
Os dois registros são gravados na mesma transação; transições concorrentes são serializadas.
Não há rotas para editar ou excluir encomendas ou eventos.

As evidências são opcionais e podem ser adicionadas em qualquer estado. O armazenamento
local requer um diretório persistente; a interface de armazenamento permite uma implementação
futura com S3, mas ela não está incluída. Como banco de dados e disco não compartilham uma
transação atômica, uma interrupção abrupta pode deixar arquivos órfãos: monitore e reconcilie
o diretório de armazenamento operacionalmente. Em falhas de commit cujo resultado seja
desconhecido, o arquivo é preservado para evitar apagar uma evidência que possa ter sido
confirmada no banco; a ocorrência é registrada para reconciliação.

São aceitas imagens JPEG, PNG, WebP e HEIC de até 10 MiB por arquivo. JPEG e PNG são
decodificadas para validação; WebP e HEIC passam por validação estrutural, não por
decodificação completa. Configure `EVIDENCE_STORAGE_DIRECTORY` (padrão `./data/evidences`),
`EVIDENCE_MAX_FILE_SIZE_BYTES` (padrão `10485760`) e `EVIDENCE_MAX_REQUEST_SIZE` (padrão
`11MB`). O limite da requisição inclui os cabeçalhos multipart e deve ser ajustado junto ao
limite do arquivo. Em ambientes com contêineres, monte um volume persistente no diretório
configurado.

### Migração do histórico

A migração Flyway V3 exige que as tabelas `deliveries` e `delivery_evidences` estejam vazias.
Se houver dados, ela falha sem excluí-los; faça backup e limpe os dados manualmente antes da
migração, caso apropriado. Não é feita reconstrução automática do histórico anterior.

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

## ROLE dos funcionários

Cada funcionário possui uma `role`: `DOORMAN` (porteiro) ou `ADMIN`. O campo é obrigatório
no corpo das requisições de criação e atualização de funcionários e é retornado em suas
consultas e na resposta de login. Os clientes existentes precisam incluir o campo no
`POST` e no `PUT` de `/api/v1/condominiums/{condominiumId}/staff-members`.

Exemplo de criação:

```json
{
  "condominiumId": "00000000-0000-0000-0000-000000000001",
  "userId": null,
  "fullName": "Funcionário",
  "role": "DOORMAN"
}
```

Exemplo de atualização: `{"fullName":"Funcionário","active":true,"role":"ADMIN"}`.

A migração V4 classifica funcionários existentes como `DOORMAN`, sem excluir registros.
Nesta etapa, a ROLE é apenas cadastral: não altera as permissões e `ADMIN` não recebe
privilégios adicionais. Qualquer funcionário que já possa alterar cadastros continua
podendo atribuir qualquer uma das duas ROLEs.

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
