# Currency converter

Keyboard-driven конвертер валют с калькулятором (в стиле Raycast): `10 usd - 10% to eur`.
Compose Multiplatform: Android + desktop (JVM), сборка в Termux на телефоне. Меньше зависимостей, проще решения,
при выборе предпочитать кросс-платформенные варианты.

## Структура
- `shared/` — всё приложение. `commonMain` — логика и UI на чистом Kotlin, без `java.*` и `android.*`
  (компилятор это не проверяет: обе цели — JVM). `jvmSharedMain` — код на `java.*`, общий для Android и desktop.
  `androidMain` / `jvmMain` — платформенные `actual` и desktop `main()`.
- `androidApp/` — только `Activity`, `Application`, манифест, иконки.

## Сборка
Не собирать обе цели одной командой — телефон в свопе.
- `./gradlew :shared:jvmTest` — все тесты (на телефоне идут на jvm-цели).
- `./gradlew :androidApp:assembleDebug` — APK в `androidApp/build/outputs/apk/debug/`.
- Desktop на телефоне только компилируется; запуск на ПК — `./gradlew :shared:run`.

## Бэклог идей
Идеи живут в `BACKLOG.md`, исследования к ним — в `research/`. Это не очередь работ:
без прямой просьбы идеи не реализуем.

## Переезд на Compose Multiplatform
Этапы 0–6 сделаны; решения, ловушки и что осталось (проверка desktop на ПК) — в
`research/compose-multiplatform.md`, §7.

## Код
Мелкие связанные модели держим в одном файле, а не по файлу на класс.
