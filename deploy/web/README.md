# Links Detektor web Worker

A Cloudflare Worker with static assets that serves the Compose Multiplatform wasm build under `/links-detektor/`. It has no script of its own; `assets.base_path` mounts the distribution at that prefix.

## Local run

```bash
./gradlew :webApp:wasmJsBrowserDistribution
cd deploy/web && npm ci && npm run dev   # http://localhost:8787/links-detektor/
```

## Deploy

The release workflow (`master.yml`, job `web`) runs `npx wrangler deploy` in this folder. Manually: `npm run deploy` after building the distribution, with `CLOUDFLARE_API_TOKEN` and `CLOUDFLARE_ACCOUNT_ID` set. A manual redeploy of an old release is `npm run deploy` from a checkout of its tag, since the release workflow has no manual trigger (it would re-run the Maven Central publish).

## Routing decision

Checked locally with `wrangler dev` 4.165.0: `/links-detektor/` answers 200 `text/html`, `/links-detektor` answers 307 to `/links-detektor/`, `composeApp.js` 200, the `.wasm` file 200 `application/wasm`, composeResources fonts 200, and `/` answers 404.

The zone route (`technowolf.in/links-detektor*`) was not tested: it needs Cloudflare credentials and touches the live zone, so it is pending the owner. Until then the site uses service-binding forwarding (the Cloudflare vertical-microfrontend pattern, plan Task 16), and `wrangler.jsonc` has no `routes`. The first workers.dev deploy has not been done either.

## Cross-origin isolation

Not needed, as far as curl and headless evidence show: the local response carries no `Cross-Origin-*` headers (`no isolation headers`) and the bundle needs no threads. Checked 2026-11-19 with curl only. Chrome, Firefox and Safari were not checked by hand, so `crossOriginIsolated` and the console remain to be confirmed in a browser. If a browser reports a `SharedArrayBuffer` error, add `webApp/src/wasmJsMain/resources/_headers` with `Cross-Origin-Opener-Policy: same-origin` and `Cross-Origin-Embedder-Policy: require-corp` for `/links-detektor/*`.

## Rollback

`npx wrangler rollback` in this folder. The main site is unaffected.
