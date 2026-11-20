---
inclusion: always
---
# SariSariManager - Technical Standards

## 1. Technology Stack
The application must use the following technologies.

### Platform
- Offline-first native Android application.
- Minimum SDK: API 26 (Android 8.0). Keep the existing compileSdk, targetSdk and Java 11 configuration unless a change is necessary.
- Use `java.time` for dates and times. Core library desugaring is not required at API 26.
- Store timestamps as UTC epoch milliseconds.
- Use the Asia/Manila time zone to define "daily" and "monthly" report boundaries.

### Programming Language
- Kotlin
- Use modern Kotlin language features.
- Prefer null safety and immutable data where practical.

### User Interface
- Jetpack Compose
- Material 3
- Compose Navigation (type-safe routes)
- Use reusable composable components.
- Follow a consistent application theme.

### Local Database
- Room Database (SQLite is Room's storage engine).
- Use Room Entities, DAOs, and Database classes.
- Use database migrations when changing the database schema.
- Do not use destructive migrations for production data.

### Architecture
- MVVM (Model-View-ViewModel)
- Repository Pattern
- Separation of concerns
- Feature-based organization within the application module.

### Dependency Injection
- Dagger Hilt is the only dependency injection framework.
- Prefer constructor injection. Use field injection only where Android requires it (Application and Activity entry points).
- `SariSariApplication` uses `@HiltAndroidApp`, `MainActivity` uses `@AndroidEntryPoint`, and every ViewModel uses `@HiltViewModel`.
- Hilt modules live in `di/`. Provide the Room database and DAOs with `@Provides`. Bind repository interfaces to implementations with `@Binds`. Install modules in `SingletonComponent` unless a narrower scope is needed.
- Inject coroutine dispatchers through qualifiers so tests can replace them.
- The domain layer must not import Hilt or Android types. Use cases may use `@Inject constructor` (plain `javax.inject`).

### Asynchronous Operations
- Kotlin Coroutines
- Kotlin Flow
- StateFlow for observable UI state.
- Use suspend functions for asynchronous operations.
- Never block the main thread. Database, backup, restore, and export work runs on injected background dispatchers.

### Build and Dependency Management
- Gradle Kotlin DSL
- Use Gradle Version Catalog (libs.versions.toml) for dependency versions.
- Use KSP (not kapt) for Room and Hilt annotation processing.
- Avoid unnecessary dependencies.
- Keep dependencies compatible with the project's Android Gradle Plugin and Kotlin versions. Verify compatibility before adding or upgrading a dependency.
- Do not request the INTERNET permission in V1.

## 2. Application Architecture
Follow this general data flow:

Compose UI -> ViewModel -> Use Cases (when needed) -> Repository -> Room Database

Responsibilities:

### Presentation Layer
- Displays application screens.
- Handles user interactions.
- Observes UI state with `collectAsStateWithLifecycle`.
- Must not directly access DAOs or database entities.
- Composables are stateless where practical and receive state through hoisting.

### ViewModel Layer
- Manages screen state.
- Coordinates user actions.
- Calls domain use cases, or repositories directly for simple reads and writes with no business rules.
- Exposes one observable `UiState` per screen using StateFlow.

### Domain Layer
- Contains business rules and calculations.
- Contains use cases. Create a use case only for business rules or multi-step operations (checkout, stock receiving, stock adjustment, costing, profit calculation, backup, restore, export). Do not create pass-through use cases for simple CRUD.
- Defines repository interfaces.
- Must not import `android.*`, `androidx.*`, or Room.

### Data Layer
- Implements repository interfaces.
- Handles database, backup, and export operations.
- Converts database entities into domain models.
- Provides data to the domain layer.

## 3. Database Standards
- Room Database is the primary source of business data.
- The application must operate without internet access.
- Use foreign keys and appropriate database indices.
- Foreign keys on historical tables (sales, sale items, stock movements, purchases) use RESTRICT, not CASCADE.
- Products are archived, never hard-deleted.
- Use database transactions for operations involving multiple related records.
- Preserve historical sales and purchase information.
- Never silently overwrite completed transactions. Corrections use a void or return record that creates compensating stock movements.
- Use explicit database migrations. Enable Room schema export and commit the schema JSON files. Test migrations.
- Store monetary values as Long integers representing Philippine Peso centavos.
- Never use Float or Double for monetary calculations.
- Store stock quantities as integers in the product's base unit. Do not use Float or Double for quantities.
- Store the current stock as a cached value updated in the same transaction as each stock movement. A test must verify it equals the sum of movements.
- Store application settings (including the costing method and store name) in a Room table so they are included in backups.
- Store open and close times in a `store_sessions` table (`openedAt`, `closedAt` nullable, UTC epoch milliseconds). Only one session may be open at a time. Day grouping uses Asia/Manila.

## 4. Business Logic Standards

### Financial Calculations
- Sales revenue must use actual transaction selling prices.
- Cost of goods sold must use the historical cost snapshot recorded at the time of sale.
- Gross profit equals sales revenue minus cost of goods sold.
- Net operating profit equals gross profit minus operating expenses.
- Inventory purchases must not be double-counted as operating expenses.
- Rounding: round half-up, once, at the end of a calculation. Do not round in intermediate steps. Rounding rules must have unit tests.

### Inventory
- Inventory changes must be recorded through stock movements.
- Every stock movement must have a movement type, quantity, date, and reason or reference.
- Stock receipts also record their unit cost.
- Support different units of measurement.
- Use a consistent integer base unit for inventory calculations.
- Use explicit unit conversion factors.

### Inventory Costing
- Two methods are supported: Weighted Average Cost and Latest Purchase Cost.
- Weighted Average Cost is the default.
- The store owner selects the method in settings.
- Weighted Average Cost is quantity-weighted using existing inventory value and incoming purchase value:
  `newAverageCost = (existingQuantity * existingAverageCost + incomingQuantity * incomingUnitCost) / (existingQuantity + incomingQuantity)`
  If the existing quantity is zero or negative, the new cost equals the incoming unit cost.
- Latest Purchase Cost sets the product's current cost to the unit cost of the most recent stock receipt.
- Costing is implemented in the domain layer as a strategy selected by the setting. The stock receipt use case applies it inside the same transaction that writes the stock movement.
- Changing the method affects future receipts and sales only. It must never modify historical records. The UI must say so before the change is confirmed.
- Every completed sale item stores the cost snapshot that applied at the time of sale.
- Never recalculate historical sales using current product costs or the current costing method.

### Sales
- A completed sale must preserve its item descriptions, quantities, selling prices, and cost snapshots.
- Checkout must save the sale, sale items, and inventory deductions atomically.
- Failed transactions must not leave partially updated inventory.
- Completed sales are immutable. Use a void or return flow for corrections.

## 5. Backup, Restore, and Export (V1)
No cloud synchronization is required in V1.

### Backup
- Manual full business database backup started by the owner.
- Checkpoint the write-ahead log before copying, or use SQLite's backup mechanism, so the file is complete.
- Write the backup to a temporary file first, then deliver it to the location chosen with the Storage Access Framework (no storage permission needed).
- A backup is a single archive containing the database file and a manifest with app version, database schema version, creation time, and a SHA-256 checksum of the database file.

### Restore
- The owner picks a backup file with the Storage Access Framework.
- Validate a temporary copy before touching live data: the archive is readable, the manifest is present, the checksum matches, SQLite `PRAGMA integrity_check` passes, the expected tables exist, and the schema version is not newer than the app supports. Reject invalid files with a clear message.
- Never overwrite existing business data without explicit confirmation. The confirmation dialog must state that current data will be replaced and show the backup's creation date.
- Before replacing data, save a safety copy of the current database. If the replacement fails, restore from the safety copy.
- Apply a validated restore by staging the file and replacing the database at the next app startup, before Room opens it. Do not swap the database file while DAOs are in use.
- Backups from older schema versions are migrated by Room's normal migrations.

### CSV Export
- Export sales reports and product and inventory lists as CSV through the Storage Access Framework.
- Use UTF-8 with a byte-order mark so spreadsheet apps display the peso sign and Filipino text correctly.
- Escape fields according to RFC 4180. Prefix values that begin with `=`, `+`, `-`, or `@` to prevent spreadsheet formula injection.
- Format money from centavos using integer arithmetic. Do not use Float or Double.
- Stream large exports instead of building them in memory.

### General
- Backup, restore, and export run off the main thread, report progress, and report errors explicitly.
- Disable Android automatic cloud backup of app data (or restrict it explicitly), so the manual backup is the defined recovery path.
- Backup files are not encrypted in V1. The UI must say so. Encryption is a future option.

## 6. UI and UX Standards
- Follow Material 3 design principles.
- Prioritize simplicity and speed.
- Design for small Android screens, portrait first.
- Use reusable composables.
- Provide clear loading, empty, and error states.
- Use accessible text sizes and touch targets (minimum 48dp).
- Minimize unnecessary navigation and user input.
- Put all user-visible text in string resources. V1 ships English only. A Filipino translation is a planned later option (per-app locales), so never hardcode text.
- Format money with one shared formatter (PHP, peso sign). Parse user-entered amounts into centavos in one place.

## 7. Offline-First Requirements
- All essential store operations must work offline.
- Cashier operations must not require an internet connection.
- Product and inventory information must be available locally.
- Financial calculations must run locally.
- Backup, restore, and CSV export must work without a network.
- Cloud backup and synchronization are future features.
- Do not introduce cloud dependencies into core business operations.

## 8. Code Quality and Testing
- Use descriptive class, function, and variable names.
- Keep functions focused and manageable.
- Avoid unnecessary code duplication.
- Handle errors explicitly. Map errors to UI messages in the ViewModel.
- Avoid hardcoded business logic in UI components.
- Tests:
  - Unit tests (JUnit, coroutines-test) for business calculations and use cases, using fakes instead of mocks where practical.
  - Unit tests for both costing methods and for rounding.
  - Room tests with an in-memory database for important database operations, migrations, and the cached-stock invariant.
  - Tests for checkout atomicity and inventory deductions.
  - Tests for backup and restore with valid, truncated, corrupted, wrong-checksum, and newer-schema files.
  - Tests for CSV escaping and formula-injection protection.

## 9. Development Rules
- Implement one feature at a time.
- Review existing code before making changes.
- Do not rewrite unrelated files.
- Explain significant architectural decisions.
- Avoid unnecessary dependencies and abstractions.
- Preserve existing working functionality.
- Run appropriate tests after implementation.
- Do not implement unapproved features.
- Ask for clarification when requirements are ambiguous.
- A task is done when the project builds, relevant tests pass, a migration exists for any schema change, and no new warnings are introduced.

## 10. Future Compatibility
Future features (cloud backup and synchronization, multiple users, multiple branches, barcode scanning, receipt printing, customer credit, digital payments) must not complicate the V1 implementation.

- Use stable primary keys and `createdAt` / `updatedAt` timestamps on business records.
- Cloud backup must build on the V1 backup format without changing it.
- Do not build synchronization abstractions before they are needed.
