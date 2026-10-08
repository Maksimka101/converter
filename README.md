# Currency converter

Keyboard-driven currency converter with a calculator, Raycast-style: type an expression, get the result.

```
10 usd - 10% to eur
2 * (13 usd + 8)
5k руб в $
```

**Try it:** https://maksimka101.github.io/converter/ (installable as a PWA)

## Features
- Arithmetic, brackets and percentages mixed with currencies in one expression.
- Currency codes, symbols (`$`, `€`, `₽`) and names in English and Russian; `k` / `m` / `тыс` / `млн` scales.
- Currency autocompletion ranked by how often and how recently you use each one.
- Rates from [fawazahmed0/currency-api](https://github.com/fawazahmed0/exchange-api), cached for offline use.

## Platforms
Compose Multiplatform, one `shared` module:

| Target  | Status |
|---------|--------|
| Android | works (minSdk 26) |
| Web     | wasmJs, experimental, deployed to GitHub Pages on every push to `master` |
| Desktop | JVM, compiles, not yet run on a PC |

## Build
Requires JDK 21.

```sh
./gradlew :shared:jvmTest                         # tests
./gradlew :androidApp:assembleDebug               # APK → androidApp/build/outputs/apk/debug/
./gradlew :shared:wasmJsBrowserDistribution       # web → shared/build/dist/wasmJs/productionExecutable/
./gradlew :shared:run                             # desktop
```

The web build has to be served over HTTP, e.g. `python3 -m http.server -d <dir>`.

## Layout
- `shared/` — the whole app: `commonMain` (logic and UI in pure Kotlin), `jvmSharedMain` (code shared by
  Android and desktop), `androidMain` / `jvmMain` / `wasmJsMain` (platform actuals and entry points).
- `androidApp/` — `Activity`, `Application`, manifest, icons.
