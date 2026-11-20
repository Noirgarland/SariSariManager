---
inclusion: always
---
# SariSariManager - Product Definition

## 1. Project Overview
SariSariManager is an offline-first Android business management application designed specifically for small sari-sari stores in the Philippines.
Its primary purpose is to simplify daily store operations, product management, sales transactions, inventory monitoring, and financial tracking.
The application should be simple enough for non-technical store owners while providing reliable business management capabilities.

## 2. Target Users
- Sari-sari store owners
- Small neighborhood retailers
- Store cashiers and employees (future support)

V1 has a single owner and no login. The owner may change selling prices at the cashier.

## 3. Core Modules

### Dashboard
- Store name header and a manual Open/Close store button showing "open since" time
- Daily sales summary
- Gross profit summary
- Inventory overview
- Low-stock notifications
- Business performance indicators

### Cashier / POS
- Search products
- Add products to cart
- Modify quantities
- Change selling prices per cart line and choose the selling unit
- Calculate transaction totals
- Accept cash payments only (quick-cash buttons, change calculation)
- Void or return a completed sale
- Calculate customer change
- Record completed transactions

### Product Management
- Add, edit, and archive products
- Manage product categories
- Configure purchase costs
- Configure selling prices
- Support multiple units of measurement
- Search and filter products

### Inventory Management
- Track available stock
- Record incoming stock with its purchase cost
- Record sold products
- Record damaged and expired products
- Allow manual stock adjustments
- Maintain stock movement history
- Monitor low-stock products

### Financial Management
- Track sales revenue
- Calculate cost of goods sold
- Calculate gross profit
- Record operating expenses
- Calculate estimated net operating profit
- Generate daily and monthly reports
- Let the owner choose the inventory costing method (Weighted Average Cost or Latest Purchase Cost)

### Settings, Backup, and Export
- Edit the store name
- Choose the inventory costing method
- Create a manual full backup of all business data
- Restore from a backup file
- Export sales reports to CSV
- Export product and inventory lists to CSV

### Future Modules
- Supplier management
- Purchase management (beyond recording incoming stock)
- Customer credit (utang)
- Digital payment recording
- Cloud backup and synchronization
- Multi-user and multi-device support
- Filipino language option (V1 ships English only, with all text in string resources so a Filipino translation can be added)
- Barcode scanning
- Weekly reports, discounts, period-over-period comparison badges, and notification bell

## 4. Business Requirements
- Currency must be Philippine Peso (PHP).
- The application must support offline operation.
- Store owners must be able to edit product selling prices.
- Product purchase costs and selling prices must be tracked separately.
- Support individual, pack, box, and custom units.
- Support different purchase and selling units with appropriate conversions.
- Maintain historical transaction records.
- The owner can record the store's open and close time each day. This only records times. It does not block sales. A forgotten session can be closed manually with an editable time.
- Preserve historical purchase costs and selling prices.
- Inventory changes must be traceable.
- Completed transactions must not be silently overwritten. Corrections are made through a void or return record.
- Two inventory costing methods are available: Weighted Average Cost (default) and Latest Purchase Cost. The owner selects the method, and changing it affects only future transactions.
- The owner can manually back up all business data and restore it from a backup file. Restoring never replaces existing data without explicit confirmation, and invalid or corrupted backup files are rejected.
- The owner can export sales reports and product and inventory lists as CSV files.
- V1 requires no cloud synchronization and no internet connection.

## 5. Financial Calculation Principles
- Sales revenue is calculated from actual transaction selling prices.
- Cost of goods sold is based on the cost snapshot recorded in each sale item at the time of sale.
- Gross profit equals sales revenue minus cost of goods sold.
- Net operating profit equals gross profit minus operating expenses.
- Inventory purchases must not be counted again as operating expenses.
- Historical financial reports must not change when current product prices, costs, or the costing method are edited.
- Detailed calculation rules are defined in tech.md.

## 6. Application Design Principles
- Simple and intuitive interface.
- Fast product searching and checkout.
- Minimal steps for common store operations.
- Clear and readable financial information.
- Bottom navigation with five tabs: Home, Sell, Products, Reports, More. More holds Inventory, Expenses, and Settings.
- Responsive Android layouts.
- Consistent Material 3 design.
- Prioritize usability on budget Android devices (Android 8.0 / API 26 and newer).

## 7. Development Priorities
Development should follow this order:
1. Project foundation and architecture.
2. Database and data management.
3. Product and category management.
4. Cashier and sales transactions.
5. Inventory management.
6. Financial management and reports.
7. Settings, backup, restore, and CSV export.
8. Advanced features and cloud integration.

## 8. Development Philosophy
Build SariSariManager as a reliable, maintainable, offline-first business management system.
Implement one module at a time. Prioritize data accuracy, transaction integrity, simplicity, and long-term maintainability over unnecessary complexity.
