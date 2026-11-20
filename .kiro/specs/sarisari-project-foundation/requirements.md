# Requirements Document

## Introduction

This document defines the technical foundation of SariSariManager, an offline-first Android application for sari-sari store management. The foundation covers dependency configuration, layered architecture, Hilt dependency injection, Room database infrastructure, centralized Navigation Compose, a Material 3 design system with bottom navigation, and a testing baseline. It contains no business functionality.

The requirements follow the steering documents (`product.md`, `tech.md`, `structure.md`). Where the request and the steering documents differ, the steering documents win, and the difference is listed under "Conflicts and Decisions".

### Existing Project Analysis

| Area | Current state |
|---|---|
| Build tooling | AGP 9.5.1, Gradle 9.6.0, Kotlin 2.2.10 (plugin `kotlin-compose` only), configuration cache enabled |
| Module | Single `:app` module, package `com.example.sarisari.manager` |
| SDK | `minSdk = 24`, `targetSdk = 37`, `compileSdk = release(37)`, Java 11 |
| Version catalog | Compose BOM 2026.02.01, core-ktx 1.10.1, lifecycle-runtime-ktx 2.6.1, activity-compose 1.8.0, JUnit 4.13.2 |
| Source | `MainActivity` (plain `ComponentActivity`, `Greeting` placeholder), `ui/theme` with the default purple palette and dynamic color enabled |
| Manifest | No `Application` class, `allowBackup="true"`, no INTERNET permission |
| Tests | Only the template `ExampleInstrumentedTest` |
| Missing | Hilt, Room, KSP, Navigation, lifecycle-compose, coroutines, test libraries, `di/`, `data/`, `domain/`, `feature/`, `core/` |

### Conflicts and Decisions

| # | Conflict or gap | Resolution used in this spec |
|---|---|---|
| C1 | `minSdk = 24` in Gradle, but `tech.md` sets API 26 | Raise `minSdk` to 26 (Requirement 1). Needs your approval. |
| C2 | Request lists no KSP, but `tech.md` requires KSP and forbids kapt | Use KSP for Room and Hilt. |
| C3 | `tech.md` requires type-safe Navigation routes, which need the Kotlin Serialization plugin and library | Add them as navigation dependencies (Requirement 5). |
| C4 | Request names a "Presentation" layer, but `structure.md` uses `feature/<name>/` for screens, ViewModels and UI state | Use `feature/` (Requirement 2). |
| C5 | Existing theme is in `ui/theme`, but `structure.md` places it in `core/designsystem` | Move the theme to `core/designsystem` (Requirement 6). This is the justified move. |
| C6 | Existing dependency versions (lifecycle 2.6.1, activity-compose 1.8.0, core-ktx 1.10.1) predate the APIs required (`collectAsStateWithLifecycle`, Hilt Navigation Compose, Navigation 2.8+) | Upgrade only the entries that a new dependency requires (Requirement 1). |
| C7 | AGP 9 provides built-in Kotlin support, so KSP, the Hilt Gradle plugin and Room must be versions that support AGP 9.5.1 and Kotlin 2.2.10 | Verify compatibility before choosing versions. This is a design-phase task, and a mismatch is reported to you rather than resolved by upgrading AGP or Kotlin silently. |
| C8 | Room cannot compile a `@Database` with zero entities, but the request says to implement no business tables | Include one minimal `AppSettingEntity` (key-value). `structure.md` lists Settings as a V1 table. **Needs your decision** (see Open Questions). |
| C9 | `structure.md` says create folders only when the first file is added, but the request says to prepare DAO and entity packages | Packages exist because they contain the settings entity and its DAO. No empty placeholder folders are created. |
| C10 | Manifest has `allowBackup="true"`, but `tech.md` requires disabling automatic cloud backup | Set `allowBackup="false"` (Requirement 3). |
| C11 | `tech.md` requires injected dispatchers, which the request does not mention | Add `DispatcherModule` (Requirement 4). |
| C12 | Room tests with an in-memory database normally run as instrumented tests, which need a device or emulator | The test approach is decided in design (see Open Questions). |
| C13 | Request lists five bottom navigation destinations. `structure.md` also lists Expenses and Settings features | Only the five requested destinations are created. Expenses and Settings are deferred. |

### Open Questions

1. **C1:** Do you approve raising `minSdk` from 24 to 26?
2. **C8:** Do you approve one `AppSettingEntity` as the single foundation entity? The alternative is a throwaway entity that is later removed.
3. **C12:** Should Room tests run as instrumented tests (requires emulator or device) or as JVM tests with Robolectric (adds one dependency)?

## Glossary

- **App**: The SariSariManager Android application, package `com.example.sarisari.manager`.
- **Version_Catalog**: The file `gradle/libs.versions.toml`.
- **Build_System**: The Gradle Kotlin DSL build of the `:app` module.
- **Application_Class**: `SariSariApplication`, the Android `Application` subclass annotated with `@HiltAndroidApp`.
- **Main_Activity**: `MainActivity`, the single Activity of the App.
- **Hilt_Graph**: The Dagger Hilt dependency graph installed in `SingletonComponent`.
- **App_Database**: The Room database class `AppDatabase`.
- **Database_Module**: The Hilt module `DatabaseModule` that provides the App_Database and DAOs.
- **Repository_Module**: The Hilt module `RepositoryModule` that binds repository interfaces to implementations.
- **Dispatcher_Module**: The Hilt module `DispatcherModule` that provides qualified coroutine dispatchers.
- **Migration_Registry**: The single Kotlin file in `data/local/database/` that lists all Room migrations.
- **Schema_Directory**: The directory `app/schemas/` that holds exported Room schema JSON files.
- **Settings_Entity**: `AppSettingEntity`, a key-value Room entity with its `AppSettingDao`, used to prove the Room infrastructure.
- **Navigation_Host**: The top-level Navigation Compose host in `core/navigation`.
- **Destination**: One of Dashboard, Cashier, Products, Inventory, Reports.
- **Placeholder_Screen**: A composable for a Destination that shows the Destination title and no business functionality.
- **Bottom_Bar**: The Material 3 `NavigationBar` that switches between the five Destinations.
- **Design_System**: The theme, color scheme, typography and reusable composables in `core/designsystem`.
- **Domain_Layer**: The packages under `domain/` (model, repository, usecase).
- **Data_Layer**: The packages under `data/`.
- **Feature_Package**: A package under `feature/` that holds the screen, ViewModel and UI state for one Destination.
- **Foundation_Tests**: The unit and instrumented tests created by this spec.

## Requirements

### Requirement 1: Dependency and Build Configuration

**User Story:** As a developer, I want all foundation libraries declared in the Version_Catalog and compatible with the existing toolchain, so that the project builds reproducibly without needless upgrades.

#### Acceptance Criteria

1. THE Version_Catalog SHALL declare a version for every library and plugin that the Build_System uses and that is not already declared in the unmodified project.
2. THE Build_System SHALL declare every dependency and plugin in `app/build.gradle.kts` through Version_Catalog aliases only, with zero hard-coded version strings or coordinates.
3. THE Build_System SHALL apply KSP for Room and Hilt annotation processing and SHALL NOT apply kapt.
4. THE Version_Catalog SHALL declare the Kotlin, Compose, Material 3, Room, Hilt, Hilt Navigation Compose, Navigation Compose, Kotlin Coroutines, Kotlin Serialization, Lifecycle ViewModel Compose and Lifecycle Runtime Compose libraries.
5. THE Build_System SHALL retain AGP 9.5.1, Kotlin 2.2.10, Gradle 9.6.0, `compileSdk = release(37)`, `targetSdk = 37` and Java 11 source and target compatibility.
6. THE Build_System SHALL set `minSdk` to 26.
7. WHEN an existing library version is lower than the minimum version that a newly added library requires, THE Version_Catalog SHALL raise that existing version to the lowest released version that satisfies the requirement and SHALL NOT raise it further.
8. IF a required library or plugin has no release compatible with AGP 9.5.1 and Kotlin 2.2.10, THEN THE Build_System configuration process SHALL stop, leave AGP and Kotlin versions unchanged, and report the name and version of the incompatible artifact to the user.
9. THE Version_Catalog SHALL declare each library and plugin exactly once, with no duplicate aliases and no duplicate group and artifact coordinates.
10. THE Build_System SHALL NOT declare the INTERNET permission in any merged or source manifest.
11. WHEN the command `./gradlew assembleDebug` runs, THE Build_System SHALL complete with exit code 0 and zero errors.
12. WHEN the command `./gradlew assembleDebug` runs, THE Build_System SHALL produce zero compiler warnings beyond those produced by the unmodified project.

### Requirement 2: Layered Package Structure

**User Story:** As a developer, I want the approved folder structure created in the single app module, so that each layer has a clear home before features are built.

#### Acceptance Criteria

1. THE App SHALL remain a single Gradle module named `:app` with package `com.example.sarisari.manager`.
2. THE App SHALL place shared code in `core/designsystem`, `core/navigation` and `core/util`, data code in `data/local/database`, `data/local/dao`, `data/local/entity` and `data/repository`, domain code in `domain/model`, `domain/repository` and `domain/usecase`, screens in `feature/<destination>/`, and Hilt modules in `di/`, where each of these paths is a sub-package of `com.example.sarisari.manager`.
3. THE App SHALL contain no package that has zero source files (`.kt` or `.java`) directly or in any of its sub-packages.
4. THE Domain_Layer SHALL contain no import of any type whose fully qualified name begins with `android.`, `androidx.`, `androidx.room.`, `dagger.` or `dagger.hilt.`.
5. WHERE a Domain_Layer use case exists, THE use case SHALL declare exactly one constructor annotated with `javax.inject.Inject` and SHALL use no other injection annotation, including `dagger.hilt.*` and `dagger.*` annotations.
6. THE Domain_Layer SHALL contain a use case class only if the class implements a business rule or combines two or more repository calls or other use case calls in a single operation.
7. WHEN a Data_Layer repository returns data to the Domain_Layer, THE Data_Layer SHALL return domain model instances converted from Room entities, and no class in `data/local/entity` SHALL appear in the signature of any `domain/repository` interface or `domain/usecase` class.
8. THE Feature_Package for each Destination SHALL contain exactly one screen composable whose name ends in `Screen`, exactly one ViewModel class whose name ends in `ViewModel`, and exactly one UI state class whose name ends in `UiState`.
9. THE App SHALL contain no composable function, in any package, that references a DAO, a Room entity or the App_Database, either directly or as a parameter, property or local variable type.
10. THE App SHALL contain no business calculation (such as totals, profit, balance or stock-level computation) inside a composable function, and each such calculation SHALL be located in the Domain_Layer or a ViewModel.
11. IF a source file violates criterion 3, 4, 5, 7, 9 or 10, THEN THE App SHALL be reported as non-compliant by a code review or static check that names the file and the violated criterion number.

### Requirement 3: Application Class and Manifest

**User Story:** As a developer, I want the Application class and manifest configured for Hilt, so that dependency injection starts with the process.

#### Acceptance Criteria

1. THE Application_Class SHALL be annotated with `@HiltAndroidApp`.
2. THE Application_Class SHALL be located at `com.example.sarisari.manager.SariSariApplication`.
3. THE AndroidManifest SHALL reference the Application_Class in the `android:name` attribute of the `<application>` element, using the fully qualified class name or a relative name that resolves to `com.example.sarisari.manager.SariSariApplication`.
4. THE AndroidManifest SHALL set `android:allowBackup` to `false`.
5. THE AndroidManifest SHALL retain the following values from the existing project without modification: the launcher intent filter on Main_Activity (`android.intent.action.MAIN` action and `android.intent.category.LAUNCHER` category), the `Theme.SariSariManager` theme, the app label, the `android:icon` and `android:roundIcon` references.
6. WHEN the App is installed and launched on a device or emulator running the minimum supported Android API level or higher, THE Main_Activity SHALL start without an uncaught exception and SHALL display the Dashboard Placeholder_Screen within 5 seconds of the launch intent.
7. IF Hilt fails to build the dependency graph at process start, THEN THE App SHALL terminate at launch with a crash report, and THE build SHALL NOT report success when `@HiltAndroidApp` is missing from the Application_Class.

### Requirement 4: Dependency Injection with Hilt

**User Story:** As a developer, I want Hilt configured with its modules prepared, so that repositories, the database and dispatchers can be injected and replaced in tests.

#### Acceptance Criteria

1. THE Main_Activity SHALL be annotated with `@AndroidEntryPoint`.
2. THE ViewModel of each Destination SHALL be annotated with `@HiltViewModel` and SHALL receive all of its dependencies through its constructor, with zero field-injected or manually instantiated dependencies.
3. THE Database_Module SHALL provide exactly one App_Database instance per application process as a singleton.
4. THE Database_Module SHALL provide each DAO declared by the App_Database through its own `@Provides` function that returns the DAO from the singleton App_Database.
5. THE Repository_Module SHALL bind each existing repository interface to its single implementation with `@Binds`, with exactly one binding per repository interface.
6. THE Dispatcher_Module SHALL provide the IO, Default and Main coroutine dispatchers, each identified by its own distinct qualifier annotation, so that three qualifiers exist and no dispatcher is provided without a qualifier.
7. THE Database_Module, Repository_Module and Dispatcher_Module SHALL each be installed in `SingletonComponent` and each be located in the `di/` package.
8. WHEN a class needs a coroutine dispatcher, THE class SHALL receive the dispatcher through a qualified constructor parameter and SHALL contain zero direct references to `Dispatchers.IO`, `Dispatchers.Default` or `Dispatchers.Main`, except inside the Dispatcher_Module.
9. IF a repository interface or its implementation does not yet exist, THEN THE Hilt_Graph SHALL contain no binding for that repository, and the Repository_Module SHALL contain no `@Binds` function referencing the missing type.
10. WHEN the project compiles, THE Hilt_Graph SHALL validate with zero missing-binding errors and zero duplicate-binding errors.
11. WHEN a test replaces a module with a test module through `@TestInstallIn`, THE Hilt_Graph SHALL supply the test-provided database, repository or dispatcher in place of the production one, with zero duplicate-binding errors.

### Requirement 5: Room Database Foundation

**User Story:** As a developer, I want Room configured with schema export and a no-destructive-migration policy, so that future entities can be added safely.

#### Acceptance Criteria

1. THE App_Database SHALL be a Room database located in `data/local/database` at version 1 and SHALL list the Settings_Entity as its only entity.
2. THE Settings_Entity SHALL store a text key as its primary key (1 to 128 characters), a text value (0 to 4,096 characters), and `createdAt` and `updatedAt` timestamps as UTC epoch milliseconds in non-null `Long` fields.
3. THE `AppSettingDao` SHALL be located in `data/local/dao` and SHALL expose suspend functions for reading and writing a setting by key and a `Flow` for observing a setting by key.
4. WHEN a setting is written with a key that already exists, THE `AppSettingDao` SHALL replace the stored value and `updatedAt`, and SHALL preserve the stored `createdAt`.
5. WHEN a setting is read or observed with a key that does not exist, THE `AppSettingDao` SHALL return null and SHALL NOT throw an exception.
6. THE Build_System SHALL enable Room schema export to the Schema_Directory.
7. THE Schema_Directory SHALL be tracked by version control and SHALL NOT match any `.gitignore` rule.
8. WHEN the project compiles, THE Build_System SHALL generate the schema JSON file `1.json` in the Schema_Directory.
9. THE Migration_Registry SHALL hold the list of migrations and SHALL be passed to the Room database builder in the Database_Module.
10. THE Database_Module SHALL NOT call `fallbackToDestructiveMigration` in any build variant.
11. IF a database file has a version for which no migration path exists, THEN THE App_Database SHALL fail to open by throwing an exception to the caller and SHALL NOT delete, modify or recreate the database file.
12. THE App_Database SHALL be structured so that a new entity can be added by creating the entity class, creating the DAO, adding both to the database, incrementing the version and adding a migration, with no change to the Database_Module other than providing the new DAO.
13. THE App SHALL contain no Product, Category, Sale, SaleItem or StockMovement entity, DAO or table.
14. THE `SettingsRepository` interface SHALL be defined in `domain/repository`, and its implementation SHALL map between the Settings_Entity and a domain model without loss of key, value, `createdAt` or `updatedAt`.
15. THE App_Database SHALL run all queries off the main thread.

### Requirement 6: Material 3 Design System

**User Story:** As a store owner, I want a clean, readable and consistent look, so that the app is easy to use on a budget phone.

#### Acceptance Criteria

1. THE Design_System SHALL be located in `core/designsystem` and SHALL contain the theme composable `SariSariManagerTheme`, the color definitions and the typography definitions.
2. WHEN the theme moves from `ui/theme` to `core/designsystem`, THE App SHALL update all references to the moved files, retain the name `SariSariManagerTheme`, and leave zero files in `ui/theme`.
3. THE Design_System SHALL define one light color scheme and one dark color scheme, and no color in either scheme SHALL use the default purple template color values.
4. WHILE the device runs Android 12 (API level 31) or later and the dynamic color parameter of `SariSariManagerTheme` is `true`, THE Design_System SHALL apply the device dynamic color scheme instead of the brand color scheme.
5. THE `SariSariManagerTheme` composable SHALL default the dynamic color parameter to `false`, so that the brand color scheme shows on every device when the caller does not set the parameter.
6. THE Design_System SHALL define the Material 3 typography so that the body large, body medium and body small text styles each have a font size of at least 16sp.
7. THE Design_System SHALL provide at least one reusable composable, and each Placeholder_Screen SHALL use at least one such composable.
8. THE Design_System SHALL render every interactive element with a touch target of at least 48dp by 48dp.
9. WHEN the app displays any screen, THE App SHALL draw content edge-to-edge and SHALL apply system bar insets so that no content overlaps the status bar or the navigation bar.
10. THE App SHALL define every user-visible string in `res/values/strings.xml`, and no user-visible string literal SHALL appear in Kotlin source files.
11. THE Design_System SHALL provide at least one `@Preview` for `SariSariManagerTheme` and at least one `@Preview` for each Placeholder_Screen, and each preview SHALL render without errors in Android Studio.

### Requirement 7: Centralized Navigation

**User Story:** As a store owner, I want to switch between the five main sections from a bottom bar, so that I can reach any section in one tap.

#### Acceptance Criteria

1. THE Navigation_Host SHALL be defined in `core/navigation` and SHALL be the only Navigation Compose host in the App.
2. THE `core/navigation` package SHALL define one type-safe route for each of Dashboard, Cashier, Products, Inventory and Reports using Kotlin Serialization, so that the App has exactly five top-level routes.
3. THE `core/navigation` package SHALL define the Bottom_Bar items in one list that holds, for each Destination, the route, the label string resource and the icon.
4. THE Bottom_Bar SHALL display exactly five items, from first to last, in the order Dashboard, Cashier, Products, Inventory, Reports.
5. THE Bottom_Bar SHALL display a text label, taken from the item's label string resource, below or beside each icon.
6. WHEN the App launches, THE Navigation_Host SHALL display Dashboard as the start destination.
7. WHEN the user taps a Bottom_Bar item, THE Navigation_Host SHALL display the matching Destination within 300 milliseconds.
8. WHEN the user taps a Bottom_Bar item, THE Navigation_Host SHALL keep at most one instance of that Destination in the back stack, SHALL save the state of the Destination being left, and SHALL restore the saved state of the Destination being opened.
9. WHEN the user taps the Bottom_Bar item of the Destination that is already displayed, THE Navigation_Host SHALL keep the current Destination and its state unchanged and SHALL NOT add a back stack entry.
10. WHILE a Destination is displayed, THE Bottom_Bar SHALL show exactly one item as selected, and that item SHALL be the one matching the displayed Destination.
11. WHEN the user presses back on the Dashboard Destination, THE Main_Activity SHALL exit the App.
12. WHEN the user presses back on any Destination other than Dashboard, THE Navigation_Host SHALL display the Dashboard Destination and THE Bottom_Bar SHALL show the Dashboard item as selected.
13. THE Feature_Package of each Destination SHALL expose one navigation graph builder function, and THE Navigation_Host SHALL only call these builder functions to register Destinations.
14. THE Feature_Package SHALL NOT own a Navigation_Host or reference the Bottom_Bar.

### Requirement 8: Placeholder Screens and Presentation State

**User Story:** As a developer, I want each Destination to have a minimal screen, ViewModel and UI state, so that the presentation pattern is proven before features are built.

#### Acceptance Criteria

1. THE App SHALL provide exactly one Placeholder_Screen for each of the five Destinations: Dashboard, Cashier, Products, Inventory and Reports.
2. WHEN a Placeholder_Screen is displayed, THE Placeholder_Screen SHALL show the title of its Destination as visible text, and the title text SHALL match the title shown for that Destination in the navigation bar.
3. THE ViewModel of each Destination SHALL expose exactly one public `StateFlow` property, and the type parameter of that `StateFlow` SHALL be a single `UiState` class specific to that Destination.
4. WHEN a Placeholder_Screen is composed, THE Placeholder_Screen SHALL observe its `UiState` with `collectAsStateWithLifecycle`.
5. THE Placeholder_Screen SHALL be split into a stateful composable that obtains the ViewModel and passes the current `UiState` to a stateless composable, and the stateless composable SHALL receive the `UiState` as a parameter and SHALL NOT reference a ViewModel.
6. THE Placeholder_Screen SHALL display no business data, no input fields and no actions that change stored data, and its only visible content SHALL be the Destination title.
7. THE Main_Activity SHALL contain only the Hilt entry point, `enableEdgeToEdge`, the theme and the call to the application root composable.
8. THE App SHALL remove the template `Greeting` composable and `GreetingPreview` from `MainActivity.kt`, so that neither declaration exists anywhere in the source tree.
9. IF the ViewModel of a Destination has not yet emitted a state, THEN THE Placeholder_Screen SHALL still display the Destination title, because the `StateFlow` SHALL hold an initial `UiState` value at creation.

### Requirement 9: Testing Foundation

**User Story:** As a developer, I want test dependencies and initial tests in place, so that later features are built with tests from the start.

#### Acceptance Criteria

1. THE Build_System SHALL declare the JUnit 4, Kotlin Coroutines Test and Room Testing libraries in the dependency configurations of the JVM test source set (`test`) and the instrumented test source set (`androidTest`), with Room Testing declared in `androidTest` and in `test` only where a JVM Room test requires it.
2. THE Foundation_Tests SHALL include at least 1 ViewModel test that injects a test dispatcher through the qualified dispatcher parameter and asserts that the initial `UiState` of 1 Destination ViewModel equals the `UiState` value defined for that ViewModel's initial state.
3. THE Foundation_Tests SHALL include 1 reusable JUnit rule or helper that sets the Main dispatcher to a test dispatcher before each test and resets the Main dispatcher after each test, including after a test that fails.
4. THE Foundation_Tests SHALL include at least 1 Room test that uses an in-memory App_Database, writes 1 setting through the `AppSettingDao`, reads it back by key, and asserts that the key and value of the result equal the key and value written.
5. THE Foundation_Tests SHALL include at least 1 Room test that writes a setting with key K and value V1 through the `AppSettingDao`, writes a setting with the same key K and value V2 (V2 differs from V1), and asserts that reading key K returns V2 and that the App_Database holds exactly 1 row for key K.
6. THE Foundation_Tests SHALL include at least 1 test that asserts all of the following: the Migration_Registry configures no destructive fallback, the Migration_Registry migrations form a chain in which each migration's end version equals the next migration's start version, the first migration starts at version 1, and the last migration ends at the App_Database version. IF the App_Database version is 1, THEN the test SHALL assert that the Migration_Registry contains 0 migrations.
7. THE Foundation_Tests SHALL include at least 1 navigation test that asserts the Bottom_Bar list contains exactly 5 Destinations, in the same order as defined in Requirement 7, with no duplicates.
8. WHERE a Foundation_Test needs a test double, THE Foundation_Tests SHALL use a hand-written fake class and SHALL NOT use a mocking framework.
9. WHEN the command `./gradlew testDebugUnitTest` runs, THE Build_System SHALL execute all JVM Foundation_Tests and finish with exit code 0 and 0 failed tests.
10. WHERE Room tests run as instrumented tests, THE Build_System SHALL execute them with `./gradlew connectedDebugAndroidTest` on a connected device or emulator and finish with exit code 0 and 0 failed tests.
11. WHEN the command `./gradlew connectedDebugAndroidTest` runs, THE existing `ExampleInstrumentedTest` SHALL pass with 0 failures.
12. IF a Foundation_Test fails, THEN THE Build_System SHALL finish with a non-zero exit code and report the name of each failed test.

### Requirement 10: Development Quality

**User Story:** As a maintainer, I want the foundation to stay small and consistent, so that later modules can be added without cleanup.

#### Acceptance Criteria

1. THE App SHALL name every class, function and variable in the source set `app/src/main` and `app/src/test` according to the naming conventions in `structure.md`, with 0 violations of those conventions.
2. THE App SHALL contain 0 abstractions, base classes or interfaces that have fewer than 1 implementation or fewer than 1 consumer.
3. THE App SHALL contain 0 dependency declarations that appear more than once, counted by identical group and artifact, across the Version_Catalog and `app/build.gradle.kts`.
4. THE App SHALL contain 0 business logic statements in composables and 0 references to a DAO from any screen composable or screen-level class.
5. THE App SHALL retain the application ID, namespace, launcher icon resources, `app_name` string value and `Theme.SariSariManager` theme resource with values identical to those present before the foundation work began.
6. THE App SHALL retain the `release` build type optimization settings with values identical to those present before the foundation work began.
7. THE project SHALL contain 0 files other than the files that Requirements 1 to 9 require, excluding build output and files ignored by `.gitignore`.
8. IF a check in criteria 1 to 7 finds a violation, THEN THE project SHALL be reported as failing Development Quality, and the report SHALL identify each violating file and the criterion number it violates.

### Requirement 11: Out of Scope

**User Story:** As a product owner, I want the foundation limited to infrastructure, so that business features are added only after their own approved specs.

#### Acceptance Criteria

1. THE App SHALL contain no source code that implements cashier or checkout logic, product create, edit or archive logic, inventory calculation, financial report calculation, supplier management, customer credit, cloud synchronization, backup, restore or CSV export.
2. THE App SHALL contain no Expenses screen and no Settings screen, and THE Bottom_Bar SHALL contain no item that navigates to an Expenses screen or a Settings screen.
3. THE App SHALL declare no dependency on a network library in its build configuration, and THE App SHALL contain no source code that opens a network connection.
4. THE App SHALL NOT declare the Android internet permission in its manifest.

### Requirement 12: Delivery Verification

**User Story:** As a project owner, I want proof that the foundation builds and runs, so that I can approve it as the base for the next module.

#### Acceptance Criteria

1. WHEN the foundation is complete, THE Build_System SHALL complete `./gradlew assembleDebug` with exit code 0, zero compilation errors, and zero failed tasks.
2. WHEN the foundation is complete, THE Build_System SHALL complete `./gradlew testDebugUnitTest` with exit code 0, zero failed tests, and at least one executed test.
3. WHEN the foundation is complete, THE App SHALL install and launch on an emulator or device running Android 8.0 (API 26) or later, and SHALL display the Dashboard Placeholder_Screen with the Bottom_Bar within 5 seconds of launch.
4. WHEN the user taps any of the five Bottom_Bar items on a running App, THE App SHALL display the matching Placeholder_Screen within 1 second and SHALL show that Bottom_Bar item as selected.
5. WHEN the device rotates or the process is recreated, THE App SHALL restore the previously selected Destination and SHALL display its Placeholder_Screen with the Bottom_Bar.
6. IF `./gradlew assembleDebug` or `./gradlew testDebugUnitTest` fails after an increment, THEN THE implementation SHALL fix the failure and re-run the Build_System with a passing result before starting the next increment.
7. THE implementation SHALL proceed in increments and SHALL run the Build_System after each increment, in the order: dependencies, Hilt and Application_Class, Room, Design_System, navigation and screens, tests.