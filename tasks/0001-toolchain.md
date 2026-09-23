---
id: 1
title: Проверить тулчейн: hello-world на Compose в Termux
status: done
priority: high
tags: [build]
depends: []
created: 2026-09-22
updated: 2026-09-22
---
## Суть
До любой логики убедиться, что минимальное Compose-приложение собирается в Termux.

## Обсуждение / варианты
- Android SDK уже есть в ~/android-sdk, x86-тулы заменены на aarch64 из Termux (aapt2 и др.).

## Решение
Закрыто 2026-09-22: тулчейн рабочий, сборка проходит. Отдельную установку hello-world по adb
не делаем — реальное приложение поедет на телефон в рамках #13, там же проверится и запуск.
Память Gradle и Kotlin daemon отдельно не замеряли: сборка идёт, вернёмся, если начнёт мешать.

## Как реализовано
Тестовый проект `~/compose-test` (вне репо) собирается.
- Версии: AGP 9.4.1 (встроенный Kotlin, без kotlin-android), плагин compose 2.4.20, Gradle 9.7.1,
  compose-bom 2026.09.00, activity-compose 1.13.0, compileSdk 37, minSdk 26, JDK 21, Kotlin DSL.
- Холодная сборка ~2 мин, инкрементальная ~9 с. Debug APK 11.5 МБ.
