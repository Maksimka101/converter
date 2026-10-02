# План: перевод на Compose Multiplatform

Цели: **Android + desktop (JVM)**. iOS и web — позже и только через CI: на телефоне они не собираются.

## 1. Что позволяет среда (Termux, aarch64)
- **Android** — собирается как сейчас (`aapt2` из Termux через override).
- **Desktop (JVM)** — компилируется, тесты логики идут через `:shared:jvmTest`. Окно запустить
  нельзя: Skiko не грузится (проверено, §6). Запуск и пакеты — на ПК или в CI.
- **iOS** — нет: у Kotlin/Native нет компилятора под linux-arm64, и нужен Xcode. Цель включать флагом в CI на macOS.
- **Web (wasm)** — Gradle качает свои Node/Binaryen под glibc. Можно пробовать с `pkg install nodejs binaryen`,
  но это тяжело; тоже в CI.
- **Память** — телефон в свопе. Максимум два модуля, не собирать обе цели одной командой:
  повседневно `:androidApp:assembleDebug` и `:shared:jvmTest`.

## 2. Что мешает в коде
- `java.math.BigDecimal` — движок, `Num`, JSON курсов. Самое больное место.
- `java.time` — `Instant`, `Clock`, `Duration`, `LocalDate`.
- Файлы и сеть — `FileRatesCache`, `httpGet` на `HttpURLConnection`.
- Локаль и формат — `DecimalFormat`, `java.util.Currency`, `localeSeed(Locale)`.
- `Stop` — конструктор `Exception(null, null, false, false)` есть только на JVM.
- Android в UI — `SharedPreferences` (2 места), `ClipData`, `dynamic*ColorScheme`, `R.string` (30 строк),
  `Application`/`Activity`.
- Тесты — JUnit4, включая `assertThrows` и `TemporaryFolder`.

Сам UI (insets, haptics, `Dialog`, material3) должен переехать почти без правок — проверяется по ходу.

## 3. Целевая структура
```
shared/      commonMain  логика + UI
             jvmShared   файлы, HTTP, формат, локаль (общий код android + desktop)
             androidMain / jvmMain
             commonTest / jvmTest
androidApp/  Activity, Application, манифест, иконки
```
При AGP 9 application и KMP в одном модуле нельзя, поэтому `shared` — на `com.android.kotlin.multiplatform.library`.
Desktop `main()` живёт в `shared/jvmMain`, без третьего модуля.

## 4. Этапы
0. **Спайк в ветке.** Пустой `shared` с целями android + jvm и Compose Resources. Проверить:
   - версии: CMP с Kotlin 2.4.20 и AGP 9.4.1 (возможен откат AGP);
   - работает ли override `aapt2` с KMP-плагином;
   - видит ли `jvmShared` `java.*` (запасной вариант — общий `srcDir` для двух целей);
   - время и память сборки против нынешних. Здесь решаем, идём дальше или нет.
1. **Убрать JVM из логики, не трогая Gradle.**
   - `Instant`/`Clock`/`Duration` → `kotlin.time`, `Locale.ROOT` → обычный `lowercase()`.
   - Файлы, HTTP, формат чисел и имена валют — за интерфейсы.
   - `BigDecimal` → свой `Decimal` (см. §5).
2. **Тесты на `kotlin.test`.** Почти механически; тесты файлов и формата остаются JVM-тестами.
3. **Разрезать на `shared` + `androidApp`.** Перенос файлов без правок логики; APK ведёт себя как раньше.
4. **UI в common.**
   - Строки → Compose Resources.
   - Буфер обмена и динамические цвета → `expect/actual`.
   - `SharedPreferences` → маленький `KeyValueStore` (на desktop — `java.util.prefs`), без библиотек.
5. **Desktop-вход.** `main()` с окном, обновление курсов при старте, нумпад скрыт. На телефоне только компиляция.
6. **CI и документация.** GitHub Actions для запуска и пакетов desktop (и iOS/web, если понадобятся);
   обновить `CLAUDE.md`.

## 5. Принятые решения
1. **Decimal** — свой класс `core/Decimal.kt` поверх `java.math.BigDecimal`; на этапе 3 становится
   `expect class` с `actual` в `jvmShared`. Для iOS/web потом — своя реализация или `ionspin/bignum`.
2. **LocalDate** — `kotlinx-datetime` 0.7.1 (та же версия, что приезжает с CMP material3); время — `kotlin.time`.

## 6. Результаты спайка (2026-10-02, ветка `cmp-spike`)
Модуль `shared` (android + jvm, Compose Resources, `jvmShared`) подключён к `app`. Вердикт: **идём дальше**.

- **Версии совместимы**: Kotlin 2.4.20, AGP 9.4.1 (`com.android.kotlin.multiplatform.library`), CMP 1.12.1,
  CMP material3 1.9.0 (= androidx material3 1.4.0). Откат AGP не нужен. Зависимости — прямыми координатами
  `org.jetbrains.compose.*`.
- **`aapt2`-override работает** с KMP-плагином; строки Compose Resources попадают в APK
  (`assets/composeResources/…/strings.commonMain.cvr`), в том числе после R8.
- **`jvmShared` видит `java.*`** и компилируется для обеих целей. Ловушка: `withAndroidTarget()` молча не
  подхватывает цель нового плагина — код уходил только в desktop. Рабочий вариант:
  `withCompilations { it.platformType == KotlinPlatformType.androidJvm }`.
- **API текущего UI есть в common**: `WindowInsets.ime/safeDrawing`, `LocalClipboard`, `LocalHapticFeedback`
  (`Confirm`, `VirtualKey`, `LongPress`), `LocalSoftwareKeyboardController`, `kotlin.time.Instant/Clock`.
  `ClipEntry` создаётся по-разному на платформах (на desktop — experimental) → `expect/actual`.
- **Skiko в Termux не грузится** (`dlopen failed: library "libGL.so.1" not found`): ни окна, ни рендера в
  картинку, ни UI-тестов desktop на телефоне. Тесты логики на jvm-цели работают.
- **Время** (тёплый демон): полная пересборка APK + тесты — 27 с вместе с `shared` (до спайка 55 с с холодным
  демоном); правка в `shared` → APK 3 с; правка → `:shared:jvmTest` 4 с; release с R8 — 113 с.
- **Память**: Gradle-демон ~2,1 ГБ, Kotlin-демон ~1,3 ГБ — как и раньше, отдельной настройки не понадобилось.
- **Размер**: debug-APK 11,9 → 12,0 МБ; release (R8, без подписи) 1,3 МБ.
- `kotlinx-datetime` 0.7.1 уже приезжает транзитивно с CMP material3 — на вес сборки выбор в §5.2 не влияет.
- Мелочи: API иерархии source set'ов в KGP помечен experimental (предупреждения при конфигурации);
  AGP просит `withHostTest {}` для `commonTest` — не нужно, тесты гоняем на jvm-цели.

## 7. Ход работ
- **Этап 0** — сделан (§6).
- **Этап 1** — сделан (2026-10-02): `java.math` только в `Decimal`, `java.time` убран.
- **Этап 2** — сделан (2026-10-02): тесты на `kotlin.test`, JUnit как прямая зависимость убран.
  Ловушка: у `kotlin.test` сообщение — последний аргумент, у JUnit — первый; `assertEquals(msg, a, b)` со
  строками компилируется молча. `assertNotNull` возвращает значение — тест-выражение перестаёт быть `void`.
- **Этап 3** — сделан (2026-10-02): `app` → `androidApp`, логика в `shared`. `Decimal` и `Stop` — `expect class`
  с `actual` в `jvmSharedMain` (флаг `-Xexpect-actual-classes`). Из common убраны последние JVM-вызовы:
  `Character.UnicodeScript`, `Character.charCount`, `putIfAbsent`.
- **Этап 4** — сделан (2026-10-02): весь UI и `App()` в `commonMain`, строки в Compose Resources
  (`app_name` остался в `androidApp`). Контракты:
  - `ValueFormatter`, `CurrencyDirectory`, `localeSeed()` — `expect` в common, `actual` в `jvmSharedMain`;
  - `ui/Platform.kt` — буфер обмена, цвета темы, ключ размера окна, `hasScreenKeyboard`;
    `actual` в `androidMain` и `jvmMain`;
  - `KeyValueStore` (строки) — `SharedPrefsStore` на Android, `JavaPrefsStore` на desktop; платформа передаёт
    хранилища и репозиторий курсов в `App()`. Старые значения SharedPreferences (Boolean/Int) читаются строкой.
- **Этап 5** — сделан (2026-10-02), **только скомпилирован**: `shared/jvmMain/Main.kt` — окно, обновление курсов
  при запуске, кэш в каталоге ОС; numpad и его переключатель скрыты (`hasScreenKeyboard = false`).
  На ПК не запускался: `./gradlew :shared:run`.
- **Этап 6** — документация обновлена (`CLAUDE.md`). От CI отказались (у репозитория нет remote).
  Для пакетов desktop в `shared/build.gradle.kts` есть блок `nativeDistributions` (версия пакета `1.0.0`: dmg не
  принимает major 0; модули `java.prefs` и `jdk.crypto.ec` для jlink указаны вручную); сборка на ПК —
  `./gradlew :shared:packageDistributionForCurrentOS`, не проверялась.

Проверка на телефоне: 219 тестов в `:shared:jvmTest` (включая тесты UI-логики), debug- и release-APK собираются.
Не проверено запуском: APK после этапа 4 (строки из Compose Resources, настройки через `KeyValueStore`) и desktop.

Что знать дальше:
- `commonMain` компилируется только под JVM-цели, поэтому JVM-API в common компилятор не ловит
  (`compileCommonMainKotlinMetadata` пропускается). Перед iOS/web — проверять на цели не-JVM.
- Тесты с `runBlocking` (`CachedRatesRepository`, `FawazRatesProvider`) лежат в `jvmTest`; в `commonTest` они
  переедут с `kotlinx-coroutines-test`, когда появится цель не-JVM.
- В `androidApp` из зависимостей — только `activity-compose` и Compose BOM. BOM не убирать: без него
  `material-ripple` приезжает 1.9.3 (от material3 1.4.0) при `ui`/`foundation` 1.12.1.