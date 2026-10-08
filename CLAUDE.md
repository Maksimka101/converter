# Currency converter

Keyboard-driven конвертер валют с калькулятором (в стиле Raycast): `10 usd - 10% to eur`.
Compose Multiplatform: Android + desktop (JVM), сборка в Termux на телефоне. Меньше зависимостей, проще решения,
при выборе предпочитать кросс-платформенные варианты.

## Структура
- `shared/` — всё приложение. `commonMain` — логика и UI на чистом Kotlin, без `java.*` и `android.*`
  (это проверяет только компиляция wasm-цели, android и desktop — обе JVM). `jvmSharedMain` — код на `java.*`, общий для Android и desktop.
  `androidMain` / `jvmMain` — платформенные `actual` и desktop `main()`.
  `wasmJsMain` — web (эксперимент): `actual` на API браузера, своя арифметика `DigitDecimal`, `main()`.
- `androidApp/` — только `Activity`, `Application`, манифест, иконки.

## Сборка
Не собирать обе цели одной командой — телефон в свопе.
- `./gradlew :shared:jvmTest` — все тесты (на телефоне идут на jvm-цели).
- `./gradlew :androidApp:assembleDebug` — APK в `androidApp/build/outputs/apk/debug/`.
- `./gradlew :shared:wasmJsBrowserDevelopmentExecutableDistribution` — web в
  `shared/build/dist/wasmJs/developmentExecutable/` (раздавать по HTTP: `python3 -m http.server -d <каталог>`).
  Production — `./gradlew :shared:wasmJsBrowserDistribution`, в `.../productionExecutable/` (~3,5 минуты).
  В Termux берутся системные `node`/`npm` и `wasm-opt` (`pkg install binaryen`).
  Тесты в wasm не идут: Skiko в Node не грузится.
  PWA: `manifest.json` и `sw.js` в `wasmJsMain/resources`; список файлов для офлайн-кэша в `sw.js` вписывает
  сборка дистрибутива. Иконки `icon-*.png` получены из `icon.svg` через `rsvg-convert`.
  Публикация: пуш в `master` собирает production в GitHub Actions и выкладывает на Pages
  (`.github/workflows/pages.yml`).
- Desktop на телефоне только компилируется (Skiko в Termux не грузится); запуск на ПК — `./gradlew :shared:run`,
  пакет — `./gradlew :shared:packageDistributionForCurrentOS`. На ПК ещё не запускался и не собирался.

## Бэклог идей
Идеи живут в `BACKLOG.md`, исследования к ним — в `research/`. Это не очередь работ:
без прямой просьбы идеи не реализуем.

## Код
Мелкие связанные модели держим в одном файле, а не по файлу на класс.
