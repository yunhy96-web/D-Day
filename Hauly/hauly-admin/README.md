# Hauly Admin

React + Vite admin SPA for the Hauly 한-태 대리구매 플랫폼.

## Prerequisites

- Node.js >= 20
- Backend running at `localhost:8090` (`hauly-backend`, Spring Boot with `local` profile)

## Setup

```bash
cd hauly-admin
npm install
```

## Running locally

```bash
npm run dev
```

Opens at http://localhost:5173. The Vite dev server proxies all `/api/*` calls to `http://localhost:8090` — no CORS configuration needed.

## Log in

Use the bootstrap test credentials (already seeded in the DB):

| Role   | Email               | Password       |
|--------|---------------------|----------------|
| INTAKE | intake@hauly.local  | changeme-12345 |
| BUYER  | buyer@hauly.local   | changeme-12345 |

Navigate to http://localhost:5173/login, enter credentials, and you'll land on the dashboard showing your user info and role.

## Other scripts

```bash
npm run typecheck   # TypeScript check (no emit)
npm run build       # tsc + Vite production build → dist/
npm run preview     # Preview the production build locally
```

## Environment variables (optional)

Create `.env.local` to override defaults:

```env
# Override backend URL for production builds (default: /api via Vite proxy)
VITE_API_BASE_URL=https://api.hauly.com/api
```

## Architecture notes

- Auth relies entirely on **HttpOnly cookies** (`hauly_at` / `hauly_rt`) — no tokens in localStorage.
- Access token from the login response body is stored in-memory via React Query cache only.
- On 401, the axios interceptor attempts one silent refresh; on failure it redirects to `/login`.
- i18n uses static bundles (ko/en/th). Language persists in `localStorage['hauly.lang']`.
  - TODO: swap to `i18next-http-backend` when `/api/i18n/messages` endpoint is ready (see `src/lib/i18n/index.ts`).
