# Cafetron Backend: Complete Feature List & API Topics

## System Overview
**Cafetron** is a corporate canteen pre-order platform that enables users to browse a menu, place orders with wallet-based payments, receive QR codes for pickup, and allows vendors to accept/decline orders with real-time status tracking.

---

## 📦 Module 1: Identity & Access Control (Auth & User Management)

### Features
- **User Registration** - Email-based signup with password hashing (BCrypt)
- **User Authentication** - Login with JWT token generation
- **Role-Based Access Control** - Support for USER, VENDOR, ADMIN roles
- **JWT Token Management** - Secure token validation and user context extraction
- **User Profiles** - View and update user information
- **Password Management** - Secure password change/reset

### API Endpoints
```
POST   /api/auth/register          → Register new user (email, password, name)
POST   /api/auth/login             → Login user, return JWT token
GET    /api/users/me               → Get authenticated user's profile
PATCH  /api/users/me/password      → Change password (authenticated users)
```

### Key Entities
- **User** - Stores email, passwordHash, fullName, role, enabled status
- **Role** - ENUM: USER, VENDOR, ADMIN

### Security Features
- **JWT Authentication Filter** - Validates tokens on every request
- **Spring Security Integration** - @PreAuthorize annotations for authorization
- **Encrypted Passwords** - BCrypt hashing for all passwords
- **Role Claims in JWT** - Carries user role for authorization checks
- **CORS Configuration** - Allows requests from frontend applications

---

## 📋 Module 2: Menu Management

### Features
- **Menu Item Creation** - Vendors create menu items with price and stock
- **Menu Item Browsing** - Users view today's available menu
- **Menu Item Search** - Search items by name/keywords
- **Menu Item Filtering** - Filter by food type (VEG, NON-VEG, etc.)
- **Stock Management** - Track and update inventory levels
- **Availability Control** - Manually toggle item availability
- **Item Categorization** - Organize items by food type
- **Vendor Association** - Each item linked to a vendor

### API Endpoints
```
POST   /api/menu                   → Create new menu item (vendor only)
GET    /api/menu                   → Get all items (full inventory)
GET    /api/menu/today             → Get today's available items
GET    /api/menu/{id}              → Get single item details
GET    /api/menu/search?name=dosa  → Search items by name
GET    /api/menu/filter?type=VEG   → Filter by food type
PUT    /api/menu/{id}              → Update menu item details
PATCH  /api/menu/{id}/stock        → Update stock level
PATCH  /api/menu/{id}/availability → Toggle item visibility
DELETE /api/menu/{id}              → Remove item from menu
```

### Key Entities
- **MenuItem** - id, name, price, stock, foodType, isAvailable, vendor_id
- **Vendor** - id, name, email, phone, contactPerson, isActive

### Data Integrity Features
- **Pessimistic Locking** - Stock updates protected with database locks
- **Inventory Checks** - Validate sufficient stock before order placement
- **Automatic Unavailability** - Item marked unavailable when stock reaches 0

---

## 🛒 Module 3: Shopping Cart & Order Placement (Most Complex)

### Features
- **Server-Side Cart** - Store user cart items with quantities
- **Add to Cart** - Add menu items with quantity
- **Remove from Cart** - Delete items from cart
- **View Cart** - See current cart contents with total
- **Order Placement** - Convert cart to order with validation
- **Stock Deduction** - Atomically reduce stock on order
- **Wallet Debit** - Charge user wallet for order
- **Order Status Tracking** - PENDING_VENDOR → COLLECTED/CANCELLED/TIMEOUT
- **Pickup Slot Selection** - Choose when to collect order
- **Order Timeout Handling** - Auto-refund if vendors don't respond in 30 mins

### API Endpoints
```
POST   /api/cart/items             → Add item to cart
DELETE /api/cart/items/{itemId}    → Remove item from cart
GET    /api/cart                   → Get current cart
GET    /api/cart/slots             → Get available pickup slots
POST   /api/orders                 → Place order (checkout)
GET    /api/orders                 → Get user's orders
GET    /api/orders/{orderId}       → Get order details
POST   /api/orders/{orderId}/timeout → Process timeout refund
```

### Key Entities
- **Order** - id, userId, token, overallStatus, paymentStatus, totalAmount, pickupSlot, createdAt, readyAt
- **OrderItem** - id, orderId, menuItemId, quantity, unitPrice
- **VendorOrderStatus** - id, orderItemId, vendorId, status, actionExpiresAt

### Concurrency Safety Features
- **Pessimistic Locking** - Stock checked & decremented atomically
- **Wallet Locks** - User wallet locked during payment processing
- **Sorted Lock Order** - Multiple items locked in ascending ID order to prevent deadlock
- **Transactional Integrity** - Entire order placement is all-or-nothing
- **Stock Validation** - Reject if insufficient stock available
- **Wallet Validation** - Reject if insufficient balance
- **Timeout Protection** - 30-minute vendor response window with auto-refund

---

## 💳 Module 4: Wallet & Transaction Management

### Features
- **User Wallet** - Personal account balance for pre-orders
- **Wallet Top-Up** - Add funds to wallet (simulated payment gateway)
- **Order Payment** - Debit wallet on successful order placement
- **Refund Processing** - Credit wallet on order cancellation/timeout
- **Transaction History** - Track all wallet operations with descriptions
- **Balance Verification** - Check sufficient funds before debit
- **Transaction Logging** - Audit trail of all wallet operations

### API Endpoints
```
GET    /api/wallet                 → Get current wallet balance
POST   /api/wallet/topup           → Add funds to wallet
GET    /api/wallet/transactions    → Get transaction history (paginated)
```

### Key Entities
- **Wallet** - id, userId (unique), balance (BigDecimal)
- **Transaction** - id, walletId, type (TOPUP/DEBIT/REFUND), amount, description, createdAt

### Concurrency Control
- **Pessimistic Write Locks** - Wallet locked during debit/refund operations
- **Balance Checks in Locked State** - Prevent race conditions on insufficient funds
- **@Version Optimistic Locking** - Optional version field for consistency

---

## 📸 Module 5: QR Code & Order Pickup

### Features
- **QR Code Generation** - Unique secure token per order
- **QR Code Display** - Render as PNG image via ZXing library
- **Order Queue Display** - Real-time list of pending/active orders for counter
- **QR Verification** - Scan and validate QR token at counter
- **Order Collection** - Mark order as COLLECTED when pickup completes
- **Conflict Reporting** - Flag order issues (item missing, etc.)
- **Conflict Refund** - Auto-refund when order flagged as conflict

### API Endpoints
```
GET    /api/pickup/qr/{orderId}    → Get QR code for order (PNG image or string)
GET    /api/pickup/queue           → Get list of pending/active orders (polls every few seconds)
POST   /api/pickup/verify          → Scan & verify QR token, mark collected
POST   /api/pickup/conflict/{orderId} → Flag order as conflict, initiate refund
```

### Key Features
- **Real-Time Queue** - Counter screen polls for updates (no WebSocket needed)
- **Unique Tokens** - UUID or HMAC-based secure tokens
- **Order State Validation** - Verify order is in correct state before marking collected
- **Automatic Refunds** - Conflict flag triggers wallet refund automatically

---

## 🏪 Module 6: Admin Console - Reporting & Operational Controls

### Features
- **Daily Analytics** - Orders, revenue, items sold per day
- **Top Items Report** - Best-selling items with quantities
- **Revenue Tracking** - Revenue series for date ranges (chart data)
- **Order Status Breakdown** - Count of orders in each status
- **Ordering Window Control** - Open/close order placement for entire system
- **Daily Cutoff Configuration** - Set time after which no new orders allowed
- **CSV Export** - Export any report to CSV format
- **Admin-Only Access** - All endpoints require ADMIN role

### API Endpoints
```
GET    /api/admin/reports/daily?date=YYYY-MM-DD → Orders, revenue, items sold that day
GET    /api/admin/reports/top-items?limit=10     → Top selling items
GET    /api/admin/reports/range?from=&to=        → Revenue per day series
GET    /api/admin/reports/status-breakdown       → Count per order status
POST   /api/admin/window/toggle                  → Open/close order window
PUT    /api/admin/cutoff                         → Set daily cutoff time
GET    /api/admin/config                         → Get current configuration
```

### Key Entities
- **OrderingConfig** - Single row: windowOpen (boolean), cutoffTime (LocalTime)

### Report DTOs
- **DailySummary** - date, orderCount, revenue, itemsSold
- **TopItem** - menuItemId, name, qtySold
- **RevenuePoint** - date, revenue
- **StatusCount** - status, count

### Features
- **DTO Projections** - Lightweight JPQL projections to avoid loading full entities
- **Group By Queries** - Efficient aggregation using database-level grouping
- **Role-Based Protection** - @PreAuthorize("hasRole('ADMIN')") on all endpoints

---

## 👥 Module 7: Vendor Management

### Features
- **Vendor Registration** - Create vendor accounts with contact info
- **Vendor Listing** - View all vendors or active vendors only
- **Vendor Details** - Get single vendor information
- **Vendor Updates** - Edit vendor name, email, phone, contact person
- **Vendor Activation** - Toggle vendor active/inactive status
- **Vendor Deletion** - Remove vendor from system
- **Menu Association** - Vendors can manage their menu items
- **Order Response** - Vendors accept/decline orders

### API Endpoints
```
POST   /api/vendors                 → Create new vendor (admin only)
GET    /api/vendors                 → List all vendors
GET    /api/vendors/active          → List active vendors only
GET    /api/vendors/{id}            → Get vendor details
PUT    /api/vendors/{id}            → Update vendor info
PATCH  /api/vendors/{id}/active     → Toggle active status
DELETE /api/vendors/{id}            → Delete vendor
```

### Vendor Order Management
```
GET    /api/vendor/orders           → Get orders for vendor's items (vendor only)
POST   /api/vendor/orders/{statusId}/accept   → Accept order (vendor)
POST   /api/vendor/orders/{statusId}/decline  → Decline with reason (vendor)
```

### Key Entities
- **Vendor** - id, name, email, phone, contactPerson, isActive, createdAt
- **VendorOrderStatus** - Links vendor to order items with status tracking

---

## 🔐 Core Security & Configuration

### Security Features
- **JWT Authentication** - Token-based stateless authentication
- **Role-Based Authorization** - USER, VENDOR, ADMIN roles
- **Password Hashing** - BCrypt for secure password storage
- **CORS Protection** - Configurable cross-origin requests
- **Ownership Checks** - Users can only access their own orders
- **Admin-Only Endpoints** - Administrative features protected

### Configuration
- **Database** - MySQL with JPA/Hibernate
- **Transaction Management** - Spring @Transactional with ACID guarantees
- **Exception Handling** - Custom exceptions and global error handlers
- **Pagination** - Support for paginated transaction and report data

---

## 🔄 Workflow: Complete Order-to-Pickup Flow

```
1. USER REGISTRATION & WALLET TOP-UP
   └─ User registers with email/password
   └─ User tops up wallet with funds

2. MENU BROWSING
   └─ User views today's menu (available items from active vendors)
   └─ User searches/filters items by name or food type

3. CART & CHECKOUT
   └─ User adds items to cart
   └─ User selects pickup slot
   └─ User submits order (checkout)

4. ORDER PLACEMENT (TRANSACTIONAL)
   └─ System locks menu items
   └─ System validates stock
   └─ System locks user wallet
   └─ System validates balance
   └─ System creates Order record
   └─ System creates OrderItems
   └─ System creates VendorOrderStatus (30-min response window)
   └─ System generates QR token
   └─ System debits wallet
   └─ → Returns QR code to user

5. VENDOR RESPONSE
   └─ Vendor sees pending orders in their dashboard
   └─ Vendor accepts or declines each order item
   └─ If accepted → Order status = ACCEPTED
   └─ If declined → Order status = DECLINED
   └─ If timeout (30 mins) → Order status = TIMEOUT, Auto-refund triggered

6. ORDER PICKUP (COUNTER)
   └─ Counter polls order queue for pending orders
   └─ Counter staff scans QR code
   └─ System verifies order status
   └─ Counter marks order as COLLECTED
   └─ → Customer picks up order

7. ANALYTICS & REPORTING
   └─ Admin views daily reports (orders, revenue, top items)
   └─ Admin can open/close ordering window
   └─ Admin can set daily cutoff time
```

---

## 🏗️ Architecture Highlights

### Concurrency & Conflict Prevention
1. **Pessimistic Locking** - PESSIMISTIC_WRITE locks on critical resources
2. **Sorted Lock Order** - Items locked in ascending ID to prevent deadlock
3. **ACID Transactions** - All-or-nothing order placement
4. **Stock Validation** - Real-time availability checks
5. **Wallet Validation** - Sufficient balance verification
6. **Timeout Management** - 30-minute vendor response window with auto-refund
7. **Idempotent Operations** - Refunds won't double-charge if retried

### Performance Optimizations
1. **JOIN FETCH Queries** - Prevent N+1 query problems
2. **DTO Projections** - Lightweight data transfer objects
3. **Lazy Loading** - Reduce unnecessary entity loading
4. **Index-Friendly Queries** - Optimized for database performance
5. **Pagination** - Handle large transaction/report datasets

### Data Integrity
1. **Unique Constraints** - Email, userId (wallet), orderItem_id (vendor status)
2. **Referential Integrity** - Foreign keys prevent orphaned records
3. **Cascade Operations** - Automatic cleanup on deletions
4. **Soft Deletes** - Status flags (isAvailable, isActive) instead of hard deletes
5. **Audit Fields** - createdAt, updatedAt timestamps on critical entities

---

## 📊 Database Schema Overview

### Core Tables
- **users** - User accounts with roles
- **wallets** - User wallet balances
- **transactions** - Wallet activity audit trail
- **menu_item** - Menu items with stock and pricing
- **vendor** - Vendor information
- **order** - Main order records
- **order_item** - Order line items (items in each order)
- **vendor_order_status** - Vendor response tracking
- **orderqr** - QR tokens and images

### Key Relationships
- User (1) ──→ (many) Orders
- User (1) ──→ (1) Wallet
- Wallet (1) ──→ (many) Transactions
- Vendor (1) ──→ (many) MenuItems
- Vendor (1) ──→ (many) VendorOrderStatus
- Order (1) ──→ (many) OrderItems
- OrderItem (1) ──→ (1) MenuItem
- OrderItem (1) ──→ (1) VendorOrderStatus

---

## 🚀 Technologies Used

- **Framework** - Spring Boot 4.0.6
- **Database** - MySQL with Hibernate JPA
- **Security** - Spring Security with JWT (JJWT 0.12.6)
- **QR Codes** - ZXing 3.5.3 (core + javase)
- **Java Version** - 17
- **Build Tool** - Maven
- **Testing** - Spring Boot Test (included)
- **Utilities** - Lombok for boilerplate reduction

---

## 📝 Summary: What Cafetron Backend Provides

| Category | Features |
|----------|----------|
| **User Management** | Registration, Login, Profile, Password change |
| **Authentication** | JWT tokens, Role-based access control |
| **Menu** | Browse, search, filter, manage stock, set availability |
| **Cart & Orders** | Add items, view cart, place orders with validation |
| **Payment** | Wallet top-up, debit on order, transaction history |
| **Order Tracking** | Status monitoring, timeout handling, ownership checks |
| **QR Codes** | Generation, display, verification at counter |
| **Pickup** | Queue display, real-time polling, order collection marking |
| **Vendor Portal** | View orders, accept/decline items, respond to requests |
| **Admin Console** | Daily analytics, reports, revenue tracking, configuration |
| **Concurrency** | Pessimistic locking, deadlock prevention, transaction safety |
| **Security** | Password hashing, authorization checks, ownership validation |
| **Data Integrity** | ACID transactions, stock management, wallet consistency |

---

This backend provides a **complete, production-ready platform** for corporate canteen pre-ordering with robust concurrency handling, comprehensive order management, real-time vendor responses, and detailed analytics.


