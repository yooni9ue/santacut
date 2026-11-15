# Frontend

React and TypeScript web client for SantaCut. The frontend is a separate workspace in the repository and does not depend on Spring classes or Gradle.

## Structure

```text
src/
  app/                  Application shell and page composition
  features/identity/    Participant registration and session profile
  features/analysis/    Analysis submission and SSE progress/results
  features/ranking/     Ranking and personal delivery ETA
  features/sharing/     Public result link creation and revocation API
  shared/api/           OpenAPI generated types and credentialed API client
  styles.css            Global design tokens and responsive layout
```

Keep endpoint calls in the corresponding feature `api.ts` file. Components own view state and call these functions; they should not duplicate URL construction, session handling, or CSRF handling. `shared/api/client.ts` includes the HttpOnly session cookie and fetches the CSRF token before write requests.

## Local development

Run the backend from `backend/` with `./gradlew bootRun`, then from the repository root:

```sh
npm ci
npm run dev
```

Vite serves `http://localhost:5173` and proxies API requests to `http://localhost:8080`. Override the proxy target with `VITE_API_PROXY_TARGET` in `frontend/.env.local`.

## API contract

With the backend running, refresh `src/shared/api/schema.d.ts` from its OpenAPI document:

```sh
npm run api:types
```

Commit generated type changes alongside API contract changes. `npm run check` runs TypeScript checks and `npm run build` creates the static production bundle.

The Docker build copies the production bundle into Spring Boot's static resources. Caddy then serves the same origin for the web app and API, so production does not need frontend hosting or CORS configuration as a separate service.
