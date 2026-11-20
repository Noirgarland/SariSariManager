# Implementation Plan: SariSari Project Foundation

## Overview

The foundation is built in six increments, in the order required by Requirement 12.7: dependencies, Hilt and Application class, Room, design system, navigation and screens, tests. Each increment ends with a build checkpoint that must pass before the next increment starts (Requirement 12.6). A final delivery verification task closes the plan. All code is Kotlin, as used in `design.md`.

Base package: `com.example.sarisari.manager`, under `app/src/main/java/com/example/sarisari/manager/`. Tests go under `app/src/test/java/com/example/sarisari/manager/`.

## Tasks

- [x] 1. Increment 1: Dependencies and build configuration
  - [x] 1.1 Update the version catalog `gradle/libs.versions.toml`
    - Add versions and aliases from the design "Library versions" table: KSP 2.3.14, Hilt 2.60.1 (fallback 2.59.2), Hilt Navigation Compose 1.4.0, Room 2.8.5, Navigation Compose 2.10.2, Lifecycle 2.10.0 (shared by runtime-ktx, runtime-compose, viewmodel-compose), Coroutines 1.10.2, Serialization core 1.9.0, Robolectric 4.18.1, AndroidX Test core 1.7.0, Kotest Property 5.9.1
    - Add the plugin aliases `ksp`, `hilt` and `kotlin-serialization` (Kotlin plugins use `version.ref = "kotlin"`)
    - Rename `lifecycleRuntimeKtx` to a shared `lifecycle` version and raise it from 2.6.1 to 2.10.0
    - Keep AGP 9.5.1, Kotlin 2.2.10, Compose BOM, core-ktx, activity-compose and JUnit unchanged. Declare each library and plugin exactly once
    - _Requirements: 1.1, 1.4, 1.5, 1.7, 1.9_

  - [x] 1.2 Update `build.gradle.kts` (root) and `app/build.gradle.kts`
    - Root: add `alias(...) apply false` for `ksp`, `hilt` and `kotlin-serialization`
    - App plugins: `android-application`, `kotlin-compose`, `kotlin-serialization`, `ksp`, `hilt`. Do not apply `kotlin-android` or `kapt`
    - Set `minSdk = 26`. Leave `compileSdk`, `targetSdk`, Java 11 and the `release` build type untouched
    - Add `ksp { arg("room.schemaLocation", "$projectDir/schemas") }` and `testOptions.unitTests.isIncludeAndroidResources = true`
    - Declare all dependencies through catalog aliases only: `implementation` (Hilt, Hilt Navigation Compose, Room runtime, Navigation Compose, Lifecycle, Coroutines, Serialization), `ksp` for `room-compiler` and `hilt-compiler`, `kspTest` for `hilt-compiler`, `testImplementation` (JUnit, coroutines-test, Room testing, Robolectric, AndroidX Test core, Hilt testing, Kotest Property)
    - Do not add `room-ktx` unless the build proves it is needed. Do not add the INTERNET permission
    - _Requirements: 1.2, 1.3, 1.5, 1.6, 1.10, 9.1, 10.3, 10.6_

  - [x] 1.3 Verify dependency resolution and compatibility
    - Run `./gradlew :app:dependencyInsight` for lifecycle, activity and core. Where Gradle resolves a higher version than declared, set the catalog entry to the resolved version (design F2)
    - If a library or plugin is incompatible with AGP 9.5.1 and Kotlin 2.2.10, try the documented fallback (Hilt 2.59.2). If still incompatible, stop, leave AGP and Kotlin unchanged and report the artifact name and version to the user
    - _Requirements: 1.7, 1.8_

- [x] 2. Checkpoint - Increment 1 build gate
  - Run `./gradlew assembleDebug`. It must exit with code 0, with no errors and no new warnings. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 1.11, 1.12, 12.6_

- [x] 3. Increment 2: Hilt and Application class
  - [x] 3.1 Create the Hilt modules in `di/`
    - `Qualifiers.kt`: `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` (each `@Qualifier`, `@Retention(BINARY)`)
    - `DispatcherModule.kt`: `@Module @InstallIn(SingletonComponent::class)` providing the three qualified dispatchers. This is the only file that may reference `Dispatchers.IO`, `Dispatchers.Default` or `Dispatchers.Main`
    - `ClockModule.kt`: provides `@Singleton Clock` as `Clock.systemUTC()`
    - _Requirements: 4.6, 4.7, 4.8_

  - [x] 3.2 Add the Application class and wire the manifest and activity
    - Create `SariSariApplication.kt` annotated with `@HiltAndroidApp`
    - In `AndroidManifest.xml`, add `android:name=".SariSariApplication"` and set `android:allowBackup="false"`. Keep the launcher intent filter, theme, label and icons unchanged
    - Annotate `MainActivity` with `@AndroidEntryPoint`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 3.5, 4.1_

- [x] 4. Checkpoint - Increment 2 build gate
  - Run `./gradlew assembleDebug`. The Hilt graph must validate with zero missing-binding and zero duplicate-binding errors. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 4.10, 12.6_

- [x] 5. Increment 3: Room database foundation
  - [x] 5.1 Create the database, entity, DAO and migration registry
    - `data/local/entity/AppSettingEntity.kt`: table `app_settings` with columns `key` (primary key), `value`, `created_at`, `updated_at`
    - `data/local/dao/AppSettingDao.kt`: abstract class with `getByKey`, `observeByKey` (Flow), a protected `@Upsert`, and an `open @Transaction suspend fun write(key, value, nowMillis)` that preserves `createdAt` on overwrite. Quote `key` with backticks in hand-written queries
    - `data/local/database/AppDatabase.kt`: version 1, `exportSchema = true`, `VERSION` constant, only `AppSettingEntity`
    - `data/local/database/Migrations.kt`: `ALL_MIGRATIONS: Array<Migration> = emptyArray()`
    - No Product, Category, Sale, SaleItem or StockMovement type
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6, 5.9, 5.12, 5.13_

  - [x] 5.2 Create the domain model, repository interface, implementation and mappers
    - `domain/model/AppSetting.kt` and `domain/repository/SettingsRepository.kt` with no `android.`, `androidx.` or `dagger.` imports
    - `data/repository/AppSettingMappers.kt`: `toDomain()` and `toEntity()` copying all four fields
    - `data/repository/SettingsRepositoryImpl.kt`: `@Inject constructor(dao, clock)`. `putSetting` throws `IllegalArgumentException` when the key is not 1 to 128 characters or the value exceeds 4,096 characters, then calls `dao.write(key, value, clock.millis())`. Missing keys return or emit `null`
    - _Requirements: 2.4, 2.7, 5.2, 5.5, 5.14_

  - [x] 5.3 Create the database and repository Hilt modules
    - `di/DatabaseModule.kt`: singleton `AppDatabase` built with `addMigrations(*ALL_MIGRATIONS)` and `setQueryCoroutineContext(@IoDispatcher)`, plus one `@Provides` function for `AppSettingDao`. Never call `fallbackToDestructiveMigration*` or `allowMainThreadQueries`
    - `di/RepositoryModule.kt`: one `@Binds` from `SettingsRepositoryImpl` to `SettingsRepository`
    - Confirm `app/schemas/.../1.json` is generated by the build and that no `.gitignore` rule matches `app/schemas`
    - _Requirements: 4.3, 4.4, 4.5, 4.7, 4.9, 5.7, 5.8, 5.9, 5.10, 5.15_

- [x] 6. Checkpoint - Increment 3 build gate
  - Run `./gradlew assembleDebug`. Confirm `app/schemas/com.example.sarisari.manager.data.local.database.AppDatabase/1.json` exists and `git check-ignore app/schemas` prints nothing. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 5.7, 5.8, 12.6_

- [x] 7. Increment 4: Material 3 design system
  - [x] 7.1 Move the theme to `core/designsystem`
    - Use the smart relocate tool to move `ui/theme/Color.kt`, `Theme.kt` and `Type.kt` to `core/designsystem/` so imports update automatically
    - Keep the name `SariSariManagerTheme`. Remove the empty `ui/theme` folder
    - _Requirements: 6.1, 6.2_

  - [x] 7.2 Define the brand theme and typography
    - `Color.kt`: one light and one dark brand scheme. Remove all template purple values (`0xFF6650A4`, `0xFF625B71`, `0xFF7D5260`, `0xFFD0BCFF`, `0xFFCCC2DC`, `0xFFEFB8C8`). Present the palette to the user for approval
    - `Theme.kt`: `dynamicColor` defaults to `false`. Dynamic color applies only when the parameter is `true` and `SDK_INT >= 31`
    - `Type.kt`: `bodyLarge`, `bodyMedium` and `bodySmall` at 16sp with 24sp line height
    - _Requirements: 6.3, 6.4, 6.5, 6.6_

  - [x] 7.3 Add strings and the reusable placeholder composable
    - `res/values/strings.xml`: add `destination_dashboard`, `destination_cashier`, `destination_products`, `destination_inventory`, `destination_reports`. Keep `app_name` unchanged
    - `core/designsystem/PlaceholderContent.kt`: centered `Text` taking the title string, with no interactive element
    - Add a `@Preview` for `SariSariManagerTheme`
    - _Requirements: 6.7, 6.8, 6.10, 6.11_

- [x] 8. Checkpoint - Increment 4 build gate
  - Run `./gradlew assembleDebug`. Confirm `ui/theme` no longer exists and no template purple value remains. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 6.2, 6.3, 12.6_

- [x] 9. Increment 5: Navigation and placeholder screens
  - [x] 9.1 Create routes, the destination list and bottom bar icons
    - `core/navigation/Routes.kt`: five `@Serializable data object` routes (Dashboard, Cashier, Products, Inventory, Reports)
    - `core/navigation/TopLevelDestination.kt`: the single `topLevelDestinations` list in the order Dashboard, Cashier, Products, Inventory, Reports, each with route, `labelRes` and `iconRes`
    - Five vector drawables `res/drawable/ic_nav_*.xml` (no `material-icons-extended`)
    - _Requirements: 7.2, 7.3, 7.4, 7.5_

  - [x] 9.2 Create the five feature packages
    - For each of `feature/dashboard`, `cashier`, `products`, `inventory`, `reports`: `XxxUiState` (with `@StringRes titleRes` defaulting to the matching string), `@HiltViewModel XxxViewModel` with one public `StateFlow<XxxUiState>` and no dependencies, `XxxScreen` (stateful, uses `hiltViewModel()` and `collectAsStateWithLifecycle`) with a stateless `XxxContent(uiState)` that uses `PlaceholderContent`, a `@Preview`, and `XxxNavigation.kt` with `fun NavGraphBuilder.xxxScreen()`
    - Each package holds exactly one `*Screen`, one `*ViewModel` and one `*UiState`. No feature references the host or bottom bar
    - _Requirements: 2.8, 2.9, 2.10, 4.2, 6.7, 6.11, 7.13, 7.14, 8.1, 8.2, 8.3, 8.4, 8.5, 8.6, 8.9_

  - [x] 9.3 Create the navigation host and root composable
    - `core/navigation/SariSariNavHost.kt`: the only `NavHost`, start destination `DashboardRoute`, disabled enter, exit and pop transitions, registers destinations only by calling the five feature builder functions
    - `core/navigation/SariSariApp.kt`: `rememberNavController`, `Scaffold` with a Material 3 `NavigationBar` built from `topLevelDestinations` with icon and text label, `innerPadding` passed to the host. Selection uses `hierarchy.any { it.hasRoute(...) }`. Navigate with `popUpTo<DashboardRoute> { saveState = true }`, `launchSingleTop = true`, `restoreState = true`
    - _Requirements: 6.9, 7.1, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 7.13, 12.5_

  - [x] 9.4 Trim `MainActivity`
    - Keep only `@AndroidEntryPoint`, `enableEdgeToEdge()`, `SariSariManagerTheme { SariSariApp() }`
    - Delete `Greeting` and `GreetingPreview`
    - _Requirements: 6.9, 8.7, 8.8_

- [x] 10. Checkpoint - Increment 5 build gate
  - Run `./gradlew assembleDebug`. Confirm the build passes with no new warnings and that no `Greeting` declaration remains. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 8.8, 12.6_

- [ ] 11. Increment 6: Testing foundation
  - [x] 11.1 Create test support files
    - `src/test/resources/robolectric.properties` pinning a supported SDK (36, or 37 once confirmed)
    - `testing/MainDispatcherRule.kt`: `TestWatcher` that sets the Main dispatcher to a test dispatcher before each test and resets it in `finished`
    - `testing/FakeClock.kt`: hand-written `java.time.Clock` subclass with settable time. No mocking framework
    - _Requirements: 9.1, 9.3, 9.8_

  - [x] 11.2 Write the placeholder ViewModel test
    - `feature/PlaceholderViewModelsTest`: with `MainDispatcherRule`, each of the five ViewModels has `uiState.value` equal to its default `UiState`, with the expected `titleRes`
    - _Requirements: 8.9, 9.2_

  - [x] 11.3 Write the navigation list and typography tests
    - `core/navigation/TopLevelDestinationsTest`: exactly five items, in the required order, no duplicate route or label
    - `core/designsystem/TypographyTest`: `bodyLarge`, `bodyMedium` and `bodySmall` font sizes are at least 16sp
    - _Requirements: 6.6, 7.4, 7.5, 9.7_

  - [ ]* 11.4 Write property test for mapper round trip
    - `data/repository/AppSettingMappersPropertyTest` using Kotest Property with at least 100 iterations, including empty values, 128-character keys, Unicode and SQL-sensitive characters
    - **Property 1: Entity and domain mapping round trip**
    - **Validates: Requirements 5.14, 2.7**

  - [ ]* 11.5 Write property test for write then read round trip
    - Start `data/repository/SettingsRepositoryPropertyTest` using a Robolectric in-memory `AppDatabase`, `SettingsRepositoryImpl` and `FakeClock`. Create the database once per class and call `clearAllTables()` between iterations
    - **Property 2: Write then read round trip** (covers `getSetting` and the first `observeSetting` emission)
    - **Validates: Requirements 5.3, 9.4**

  - [ ]* 11.6 Write property test for overwrite behavior
    - Add to `SettingsRepositoryPropertyTest`
    - **Property 3: Overwrite keeps one row and preserves createdAt**
    - **Validates: Requirements 5.4, 9.5**

  - [ ]* 11.7 Write property test for absent keys
    - Add to `SettingsRepositoryPropertyTest`
    - **Property 4: Absent key reads as null**
    - **Validates: Requirements 5.5**

  - [ ]* 11.8 Write property test for input bounds
    - Add to `SettingsRepositoryPropertyTest`
    - **Property 5: Out-of-bounds input is rejected without side effects**
    - **Validates: Requirements 5.2**

  - [x] 11.9 Write the concrete DAO tests
    - `data/local/AppSettingDaoTest` (Robolectric, in-memory database): one write then read by key asserting key and value, and one overwrite of key K from V1 to V2 asserting a single row with value V2
    - _Requirements: 9.4, 9.5_

  - [x] 11.10 Write the migration and database open failure tests
    - `data/local/database/MigrationsTest`: `AppDatabase.VERSION == 1`, `ALL_MIGRATIONS` is empty, and chain assertions for the general case (first start is 1, contiguous, last end equals `VERSION`). Assert the sources contain no destructive fallback
    - `data/local/database/DatabaseOpenFailureTest`: create a version 1 file, reopen with a higher version and no migration, expect an exception, and assert the file bytes are unchanged
    - _Requirements: 5.10, 5.11, 9.6_

  - [-] 11.11 Write the Hilt module replacement test
    - `di/DispatcherModuleReplacementTest`: `@HiltAndroidTest` with `HiltTestApplication` and a `@TestInstallIn` module replacing `DispatcherModule`. A test-only holder receives the three qualified dispatchers. Assert it gets the replacements with no duplicate-binding error
    - _Requirements: 4.6, 4.11_

  - [~] 11.12 Write the static source rules test
    - `SourceRulesTest` scanning `app/src/main`. Failure messages must name the file and the requirement number. Rules: no empty package (2.3), `ui/theme` absent (6.2), no `android.`, `androidx.` or `dagger.` import in `domain/` (2.4), no `data.local` import in `feature/` or `core/` (2.9, 10.4), exactly one `NavHost(` (7.1), no `Dispatchers.` outside `DispatcherModule` (4.8), no `fallbackToDestructiveMigration` or `allowMainThreadQueries` (5.10, 5.15), no `kapt` in Gradle files (1.3), no `INTERNET` in the manifest (1.10, 11.4), `allowBackup="false"` (3.4), no template purple hex values (6.3), no `Text("...")` string literals (6.10), no `Greeting` (8.8), one `*Screen`, `*ViewModel` and `*UiState` per feature (2.8)
    - _Requirements: 2.11, 10.8_

- [~] 12. Checkpoint - Increment 6 test gate
  - Run `./gradlew testDebugUnitTest`. It must exit with code 0, with at least one executed test and zero failures. Run `./gradlew assembleDebug` again. Fix any failure before continuing. Ask the user if questions arise.
  - _Requirements: 9.9, 9.12, 12.2, 12.6_

- [~] 13. Final delivery verification
  - Run `./gradlew assembleDebug` and `./gradlew testDebugUnitTest` and confirm exit code 0, no failed tasks, no new warnings
  - Check against the requirements: no Expenses or Settings screen or bottom bar item, no network library, no INTERNET permission, application ID, namespace, icons, `app_name` and theme resource unchanged, release build type unchanged, no files outside what Requirements 1 to 9 need
  - Report to the user the checks that need a device or Android Studio and cannot be run by the agent: launch on API 26+ showing Dashboard and the bottom bar within 5 s, tap each of the five items (screen within 1 s, one selected item, re-tap does not change state), back from a non-Dashboard destination returns to Dashboard, back on Dashboard exits, rotation and process recreation restore the destination, edge-to-edge insets, 48dp targets, previews render, and `./gradlew connectedDebugAndroidTest` passes `ExampleInstrumentedTest`
  - _Requirements: 1.11, 1.12, 3.6, 7.6, 7.7, 7.8, 7.9, 7.10, 7.11, 7.12, 9.11, 10.5, 10.6, 10.7, 11.1, 11.2, 11.3, 11.4, 12.1, 12.2, 12.3, 12.4, 12.5_

## Notes

- Tasks marked with `*` are optional. They are the property-based tests, which can be skipped for a faster MVP. The example tests under Requirement 9 are not optional.
- Increments run in the required order. Each checkpoint must pass before the next increment starts.
- Requirement 9.10 (instrumented Room tests) does not apply, because Room tests run on the JVM with Robolectric (design assumption A3).
- Open decisions from the design that need the user's approval: assumptions A1 to A4, the brand palette (task 7.2), Hilt 2.60.1 with the 2.59.2 fallback, rewording Requirements 1.7 and 9.2, and the extra `ClockModule`.
- No product code is written by this plan document. Tasks run when started from this file.

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1"] },
    { "id": 1, "tasks": ["1.2"] },
    { "id": 2, "tasks": ["1.3"] },
    { "id": 3, "tasks": ["2"] },
    { "id": 4, "tasks": ["3.1", "3.2"] },
    { "id": 5, "tasks": ["4"] },
    { "id": 6, "tasks": ["5.1"] },
    { "id": 7, "tasks": ["5.2"] },
    { "id": 8, "tasks": ["5.3"] },
    { "id": 9, "tasks": ["6"] },
    { "id": 10, "tasks": ["7.1"] },
    { "id": 11, "tasks": ["7.2", "7.3"] },
    { "id": 12, "tasks": ["8"] },
    { "id": 13, "tasks": ["9.1"] },
    { "id": 14, "tasks": ["9.2"] },
    { "id": 15, "tasks": ["9.3"] },
    { "id": 16, "tasks": ["9.4"] },
    { "id": 17, "tasks": ["10"] },
    { "id": 18, "tasks": ["11.1"] },
    { "id": 19, "tasks": ["11.2", "11.3", "11.4", "11.5", "11.9", "11.10", "11.11", "11.12"] },
    { "id": 20, "tasks": ["11.6"] },
    { "id": 21, "tasks": ["11.7"] },
    { "id": 22, "tasks": ["11.8"] },
    { "id": 23, "tasks": ["12"] },
    { "id": 24, "tasks": ["13"] }
  ]
}
```
