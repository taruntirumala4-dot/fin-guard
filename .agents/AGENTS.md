# FinGuard Engineering & Backend Guardrails

## Backend Architecture Rules
1. **Spring Boot Verification**: Always execute `mvn test` and `mvn compile` after modifying any Java backend file. Never declare success without green test results.
2. **Spring MVC Controller Route Ordering**:
   - Literal sub-paths (e.g. `@GetMapping("/alerts/recent")`) MUST be defined BEFORE path variables (e.g. `@GetMapping("/alerts/{id}")`).
   - Re-ordering controller methods must strictly preserve literal path precedence.
3. **Database & Entity Primary Keys (`GenerationType.IDENTITY`)**:
   - Never set explicit `.id(...)` on entity builders when inserting records into tables configured with auto-increment / `IDENTITY`.
   - `save()` on an entity with a non-null ID invokes `merge()` instead of `persist()`, causing extra `SELECT` queries and potential primary key collisions.
4. **Cache & Microservice Resilience**:
   - **Redis (`RedisService`)**: Redis is strictly an auxiliary cache. All cache reads/writes MUST be wrapped in `try-catch (Throwable ex)` so Redis downtime never crashes HTTP endpoints or database operations.
   - **ML Service Call (`WebClient`)**: Calls to external ML/microservices must enforce strict timeouts (e.g., 2-5s) and return fallback scores cleanly on timeout or connection failure.
   - **Kafka (`TransactionProducer`)**: Kafka template operations must degrade gracefully when Kafka brokers are unreachable in local profiles.
5. **JWT & Security**:
   - Use direct UTF-8 byte keys (`jwtSecret.getBytes(StandardCharsets.UTF_8)`) when building HMAC keys via `Keys.hmacShaKeyFor()`. Avoid double Base64 encoding.
6. **Error Handling**:
   - `GlobalExceptionHandler` must handle `Throwable` and return structured JSON `ErrorResponse` objects with standard `timestamp`, `status`, `error`, `message`, and `path` fields. Stack traces must never be exposed to clients.
7. **Standby / Offline Demo Mode (CRITICAL)**:
   - The app MUST NEVER block the user at the login screen due to the backend being offline.
   - `AuthContext.jsx` checks backend health every 4 seconds via `/api/health`. When `isCheckingBackend` becomes `false` and `isBackendConnected` is `false`, the login page MUST auto-enter standby mode using `loginStandby('ADMIN', ...)`.
   - If a login attempt fails with no `error.response` (network error), the app MUST auto-call `loginStandby` within 1 second and redirect to `/dashboard`.
   - All pages must gracefully handle standby mode: use `localStorage.getItem('finguard_standby') === 'true'` to return mock data instead of failing API calls.
   - Never show raw backend URLs (e.g., `http://localhost:8080`) in user-facing error messages.
