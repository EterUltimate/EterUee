# EterUee Web UI

`web-ui` is the React browser frontend embedded into the Android app. It is built as static client assets and copied into the Android `web` module, where Ktor serves it from the device.

## Runtime Path

```text
web-ui
  -> react-router build
  -> copy.ts
  -> ../web/src/main/resources/static
  -> :web Android library
  -> WebServerManager / Ktor
  -> browser on local network
```

The app does not deploy `web-ui` as a standalone hosted service for normal Android builds. Standalone dev mode is for frontend iteration only.

## Stack

| Area | Technology |
| --- | --- |
| Framework | React Router 7 |
| React | React 19 |
| Data/query | TanStack Query, ky |
| Streaming/rendering | streamdown, shiki, KaTeX, remark/rehype |
| State | Zustand, Immer |
| UI utilities | radix-ui, lucide-react, motion, Tailwind CSS |
| Validation | zod |
| Formatting/lint | oxfmt, oxlint |
| Build tool | Vite through React Router |

## Directory Map

| Path | Responsibility |
| --- | --- |
| `app/routes` | Route modules |
| `app/components` | Shared UI components |
| `app/hooks` | React hooks |
| `app/services` | API clients and service wrappers |
| `app/stores` | Client state stores |
| `app/types` | Shared TypeScript types |
| `app/locales` | i18n resources |
| `public` | Static public assets |
| `copy.ts` | Copies `build/client` into `../web/src/main/resources/static` |

## Commands

Run from `web-ui/`:

```bash
npx --yes pnpm@10.24.0 install --frozen-lockfile
npx --yes pnpm@10.24.0 run dev
npx --yes pnpm@10.24.0 run typecheck
npx --yes pnpm@10.24.0 run build
npx --yes pnpm@10.24.0 run fmt:check
```

The Android `web` module also invokes:

```bash
npx --yes pnpm@10.24.0 install --frozen-lockfile
npx --yes pnpm@10.24.0 run build
```

through `web/build.gradle.kts`.

## API Boundary

The Web UI talks to the embedded Ktor server in the Android app. Streaming chat work should stay compatible with:

- [../docs/STREAM_V2_USAGE_GUIDE.md](../docs/STREAM_V2_USAGE_GUIDE.md)
- [../docs/BACKEND_SSE_STANDARDIZATION.md](../docs/BACKEND_SSE_STANDARDIZATION.md)
- [../docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md)

Do not assume a public cloud API base URL unless a feature explicitly introduces one. The default production path is browser to local Android device.

## Build Output

React Router writes client assets under:

```text
web-ui/build/client
```

`copy.ts` replaces:

```text
web/src/main/resources/static
```

Generated output should not be committed unless the release/build process explicitly requires it.

## Development Notes

- Keep API clients typed and close to `app/services`.
- Keep route-level state in route modules; share reusable cross-route state through `app/stores`.
- Prefer the existing streaming message model over ad hoc text-only state.
- Verify text wrapping and layout in both desktop browser and mobile browser widths when changing core chat UI.
- When changing build scripts, update `web/build.gradle.kts` and [../docs/DEVELOPMENT.md](../docs/DEVELOPMENT.md).
