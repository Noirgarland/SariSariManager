---
inclusion: always
---
# SariSariManager - Project Structure and Organization

## 1. General Organization
The project uses a feature-based structure for UI, with data and domain organized by layer.
For the initial development stage, use a single Android application module.
Do not introduce multiple Gradle modules unless there is a clear architectural reason.

The main package is the existing project package:
com.example.sarisari.manager

Do not rename the package without explicit approval.

## 2. Main Project Structure
Organize the source code as follows. Create folders only when the first file that needs them is added.

```text
SariSariManager/
|-- .kiro/steering/            (product.md, tech.md, structure.md)
|-- app/
|   |-- build.gradle.kts
|   `-- src/main/
|       |-- AndroidManifest.xml
|       |-- res/
|       `-- java/com/example/sarisari/manager/
|           |-- core/
|           |   |-- designsystem/   theme and reusable UI components
|           |   |-- navigation/     routes and navigation assembly
|           |   `-- util/           shared helpers (money, dates, results)
|           |-- data/
|           |   |-- local/
|           |   |   |-- database/   Room database, migrations, converters
|           |   |   |-- dao/
|           |   |   `-- entity/
|           |   |-- backup/         backup, restore, validation
|           |   |-- export/         CSV writers
|           |   `-- repository/     repository implementations
|           |-- domain/
|           |   |-- model/
|           |   |-- repository/     repository interfaces
|           |   `-- usecase/
|           |-- feature/
|           |   |-- dashboard/
|           |   |-- cashier/
|           |   |-- products/
|           |   |-- inventory/
|           |   |-- expenses/
|           |   |-- reports/
|           |   |-- utang/          customers, balances, payments (opened from More)
|           |   `-- settings/       costing method, backup, restore, export
|           |-- di/                 Hilt modules
|           |-- MainActivity.kt
|           `-- SariSariApplication.kt
|-- gradle/libs.versions.toml
|-- build.gradle.kts
|-- settings.gradle.kts
`-- gradle.properties
```

## 3. Folder Responsibilities

### core/
Contains shared application components.
- designsystem/: Application theme, colors, typography, and reusable UI components.
- navigation/: Navigation routes and the code that assembles each feature's navigation graph.
- util/: General-purpose helpers, such as money formatting and parsing, date helpers, and result types.

### data/
Contains database and data access implementation.
- local/database/: Room Database configuration, migrations, and type converters.
- local/dao/: Database access objects.
- local/entity/: Room database entities.
- backup/: Backup creation, backup manifest, restore validation, and staged restore.
- export/: CSV writers.
- repository/: Implementations of domain repository interfaces.

### domain/
Contains business logic and application models. No Android, Room, or Hilt dependencies (plain `@Inject constructor` is allowed).
- model/: Business models independent of Room and Android UI.
- repository/: Repository interfaces.
- usecase/: Business operations and rules, including costing, checkout, stock receiving, backup, restore, and export. Create a use case only when there is a business rule or multi-step operation.

### feature/
Contains individual application modules organized by functionality.
Each feature contains its own presentation components.

Example:
```text
feature/
`-- products/
    |-- ProductScreen.kt
    |-- ProductViewModel.kt
    |-- ProductUiState.kt
    `-- components/
```
This is a guideline. Add a `components/` folder only when a feature has reusable composables.

### di/
Contains Hilt modules: `DatabaseModule`, `RepositoryModule`, `DispatcherModule`, and others as needed.

## 4. Feature Organization
Each feature must be independently organized.

### Dashboard
- Store name header, open/close store control, sales summaries, profit summaries, inventory overview, business indicators.

### Cashier
- Product searching, shopping cart, quantity management, checkout, payment calculations, sales confirmation.

### Products
- Product creation, editing, archiving, categories, price management, unit configuration.

### Inventory
- Stock monitoring, stock receiving (with unit cost), stock adjustments, damaged and expired products, stock movement history, low-stock notifications.

### Expenses
- Expense recording, expense categories, expense history.

### Reports
- Sales reports, gross profit reports, expense reports, inventory valuation, business performance summaries.

### Settings
- Store name editing.
- Costing method selection.
- Manual backup and restore, with confirmation dialogs.
- CSV export of sales reports and of product and inventory lists.

## 5. Naming Conventions
Follow standard Kotlin conventions (PascalCase for classes, camelCase for functions and variables). Project-specific suffixes:
- Room entities: `ProductEntity`
- DAOs: `ProductDao`
- Repository interface: `ProductRepository`; implementation: `ProductRepositoryImpl`
- Screens: `ProductScreen`; ViewModels: `ProductViewModel`; UI state: `ProductUiState`
- Hilt modules: `DatabaseModule`, `RepositoryModule`
- Use cases: verb phrase, for example `CompleteSaleUseCase`

## 6. Code Organization Rules
- Keep UI code inside the appropriate feature folder.
- Keep database entities separate from domain models for core business data (products, sales, stock movements). Trivial lookup data may share a model if it has no Room-specific fields.
- Keep database queries inside DAO classes.
- Keep database access behind repository implementations.
- Keep business calculations inside domain logic or use cases.
- Keep ViewModels focused on managing screen state and coordinating actions.
- Put reusable components in core/designsystem.
- Avoid duplicating business logic across features.
- Avoid creating unnecessary folders or classes.
- Do not place all application code inside MainActivity.kt.

## 7. Navigation Organization
Major application navigation is assembled centrally in core/navigation.
- The bottom navigation has five tabs: Home, Sell, Products, Reports, More. More links to Inventory, Expenses, and Settings.
- core/navigation holds the routes, bottom navigation configuration, and the top-level navigation host.
- Each feature may expose its own navigation graph builder. core/navigation only assembles them.
- Features must not independently manage the entire application navigation structure.

## 8. Database Organization
All Room-related classes must remain inside data/local.

The V1 database supports:
- Products
- Categories
- Units and unit conversions
- Sales
- Sale Items (with cost snapshot)
- Inventory Stock Movements (with unit cost for receipts)
- Expenses
- Settings (including the costing method and store name)
- Store Sessions (open and close times)

- Customers and Credit Entries (the utang ledger; a balance is the sum of a customer's entries)

Deferred until their modules are approved: Suppliers, and Purchases and Purchase Items (beyond stock receipts).

Add database entities and DAOs incrementally as each feature is implemented.

## 9. Development and Maintenance
- Follow the established folder structure.
- Reuse existing shared components.
- Avoid creating duplicate implementations.
- Keep each feature logically separated.
- Do not move or rename existing files without a reason.
- Update this document when significant architectural changes are approved.
- Keep the project understandable and maintainable for future developers.
