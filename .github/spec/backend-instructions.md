# Project instructions

Backend: Java 21+, Spring Boot 4.1.1, MVC (Controller → Service → Repository), Spring Data JPA, PostgreSQL, Flyway. Base package: `br.com.lfmelo`.

## Structure (package by layer)
`controller`, `dto`, `service`, `repository`, `entity`, `mapper`, `enums`, `exception`, `config`.

## Layer rules
- Controller: thin. Validate with `@Valid`, call a service, return DTO + status. Never touch repositories. Never expose entities. Base path `/api/v1`, plural resources.
- Service: concrete class, no interface. Holds business rules and `@Transactional` (`readOnly = true` for reads). Returns DTOs or domain values; knows nothing about HTTP.
- Repository: Spring Data JPA interfaces only. No business logic.
- Entity: JPA classes only. Never returned by the API.
- Mapper: pure functions entity <-> DTO. One mapper per type pair, no duplicated logic.
- DTO: `record` or `class`, stay consistent within a feature.
- Dependencies flow downward only (controller → service → repository → entity).

## Errors
- Services throw `NotFoundException` (404), `ConflictException` (409), `BusinessRuleException` (422).
- One `@RestControllerAdvice` maps exceptions to `ProblemDetail`. Validation errors → 400. Unexpected → 500 with generic message, details only in logs.

## JPA
- Entities: protected no-arg constructor, `equals`/`hashCode` on id, no Lombok `@Data`.
- Relations `LAZY`. Avoid N+1 with `@EntityGraph` or `JOIN FETCH`.
- `open-in-view=false`, `ddl-auto=validate`.

## Database
- Schema changes only via Flyway: `V{n}__description.sql`. Never edit an applied migration.
- `snake_case` names, `uuid` primary keys, `timestamptz` in UTC for `created_at`/`updated_at`, `numeric(19,2)` for money.
- Constraints (`NOT NULL`, `UNIQUE`, `FK`, `CHECK`) live in the DB.
- Config via env vars (`DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`). No secrets in the repo.

## Code style
- Constructor injection, `final` fields. No field `@Autowired`.
- Small methods, one responsibility (~15 lines max). Descriptive names.
- No duplicated logic: extract shared code. No twin methods for the same intent (e.g. `findById` and `getById`).
- Return `Optional`, never `null`. No `Optional` as parameter or field.
- Prefer immutability (`final`, `List.copyOf`).
- Comments explain why, not what. No "Utils" catch-all classes.
- Inject `Clock` (and id generators) instead of calling `Instant.now()` / `UUID.randomUUID()` in business logic.

## Tests
- Service: JUnit 5 + Mockito + AssertJ, repositories mocked.
- Mapper: plain unit tests.
- Controller: `@WebMvcTest` + `MockMvc`, service mocked.
- Repository: `@DataJpaTest` + Testcontainers PostgreSQL (never H2).
- Few end-to-end tests with `@SpringBootTest`.
- Name: `shouldThrowConflict_whenEmailAlreadyExists`. Arrange / Act / Assert, one concept per test.
- Every new service method gets unit tests; every new endpoint gets a controller test.

## When generating code
- Follow existing patterns in the repo before inventing new ones.
- Add only what was asked. Do not add interfaces, layers or abstractions "just in case".
- Add a Flyway migration whenever an entity changes.
