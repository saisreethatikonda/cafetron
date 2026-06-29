# Cafetron Backend: Handling Multiple Orders & Conflict Prevention

## Overview
The Cafetron backend uses a multi-layered approach combining **pessimistic locking**, **database transactions**, and **optimized query patterns** to safely handle concurrent order placement while preventing conflicts and data inconsistencies.

---

## 1. Core Concurrency Control Mechanisms

### A. Pessimistic Locking (Database-Level)

The system employs **PESSIMISTIC_WRITE locks** at critical points to ensure that concurrent requests don't interfere with each other.

#### Menu Items Lock
**Location**: `MenuItemRepository.findByIdForUpdate()`
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select m from MenuItem m where m.id = :id")
Optional<MenuItem> findByIdForUpdate(@NotNull Long id);
```

**Purpose**: 
- Acquires an exclusive database lock on a menu item before any stock modification
- Prevents race conditions where two orders might simultaneously read the same stock level
- Ensures accurate stock deduction

**How it works in order placement** (OrderServiceImpl.placeOrder, lines 72-74):
```java
MenuItem menuItem = menuItemRepository.findByIdForUpdate(itemRequest.menuItemId())
        .orElseThrow(() -> new IllegalArgumentException("Menu item not found: " + itemRequest.menuItemId()));
```
- When an order is being placed, each menu item is locked before stock is checked and decremented
- No other thread can read or write to this menu item until the transaction completes

#### User Wallet Lock
**Location**: `WalletRepository.findByUserIdForUpdate()`
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT w FROM Wallet w WHERE w.user.id = :userId")
Optional<Wallet> findByUserIdForUpdate(@Param("userId") Long userId);
```

**Purpose**:
- Prevents concurrent wallet debits from overshooting available balance
- Ensures wallet operations (debit, refund, top-up) are atomic

**Used in WalletServiceImpl**:
- `debit()` (line 37): Locks wallet before subtracting order amount
- `refund()` (line 60): Locks wallet before refunding
- `topUp()` (line 79): Locks wallet before crediting funds

---

### B. Transaction-Level Isolation

The system uses Spring's **`@Transactional`** annotation with **ACID properties** to group related operations.

#### Order Placement Transaction
**Location**: `OrderServiceImpl.placeOrder()` (lines 49-143)
```java
@Override
@Transactional
public PlaceOrderResponse placeOrder(Long userId, PlaceOrderRequest request)
```

**Transaction Scope** (all-or-nothing semantics):
1. **Validate** order items exist and have stock
2. **Lock & Deduct** menu item stocks for each item
3. **Lock & Debit** user wallet with total order amount
4. **Create** Order entity in database
5. **Create** OrderItem entries (with order association)
6. **Create** VendorOrderStatus records (30-min response window per item)
7. **Generate** QR token and update order token

**If any step fails**, the entire transaction is rolled back:
- Menu item stocks return to original levels
- Wallet balance is unchanged
- No Order record is created
- No OrderItems are created

---

## 2. Deadlock Prevention Strategy

### The Sorted Lock Order Pattern
**Location**: `OrderServiceImpl.placeOrder()` (lines 55-59)

```java
// Slice 3: sort items by menuItemId ascending to guarantee consistent lock order
// prevents deadlock when two concurrent requests share some menu items
List<PlaceOrderItemRequest> sortedItems = request.items().stream()
        .sorted(Comparator.comparing(PlaceOrderItemRequest::menuItemId))
        .toList();
```

**Why this matters**:
- **Problem**: If Thread A locks items [5, 10] and Thread B locks items [10, 5], **circular deadlock** occurs
- **Solution**: All threads acquire locks in the same order (ascending by ID)
- **Result**: Linear lock progression prevents circular wait conditions

**Example Scenario**:
```
Order 1 requests items: [100, 50, 200]
Order 2 requests items: [75, 100, 150]

After sorting:
Order 1: [50, 100, 200]
Order 2: [75, 100, 150]

Both threads lock in ascending ID order → No deadlock possible
```

---

## 3. Stock Management & Conflict Avoidance

### Atomic Stock Updates
**Location**: `OrderServiceImpl.placeOrder()` (lines 79-86)

```java
if (menuItem.getStock() < itemRequest.quantity()) {
    throw new IllegalStateException("Insufficient stock for item: " + menuItem.getItemName());
}

menuItem.setStock(menuItem.getStock() - itemRequest.quantity());
if (menuItem.getStock() == 0) {
    menuItem.setAvailable(false);
}
```

**Flow**:
1. **Lock** menu item (via `findByIdForUpdate`)
2. **Read** current stock in locked state
3. **Check** if stock is sufficient
4. **Update** stock atomically
5. **Mark unavailable** if stock reaches 0
6. **Unlock** when transaction ends

**Conflict Scenario Prevented**:
```
Initial Stock: 5

Concurrent Order A (Qty: 3)    Concurrent Order B (Qty: 3)
├─ Lock Item
├─ Read Stock: 5
├─ Check: 5 >= 3 ✓
├─ Reduce: 5 - 3 = 2
│
│                              (Waiting for lock...)
│
├─ Save Stock: 2
├─ Unlock
                               ├─ Lock Item
                               ├─ Read Stock: 2
                               ├─ Check: 2 >= 3 ✗ → REJECT
                               
Result: Order A succeeds, Order B correctly rejected
```

---

## 4. Wallet Transaction Safety

### Dual-Lock Protection
**Location**: `WalletServiceImpl.debit()` (lines 33-53)

```java
@Override
@Transactional
public void debit(Long userId, BigDecimal amount, String description) {
    Wallet wallet = walletRepository.findByUserIdForUpdate(userId)
            .orElseThrow(() -> new WalletNotFoundException(userId));
    
    if (wallet.getBalance().compareTo(amount) < 0) {
        throw new InsufficientFundsException(wallet.getBalance(), amount);
    }
    
    wallet.setBalance(wallet.getBalance().subtract(amount));
    walletRepository.save(wallet);
    
    Transaction transaction = new Transaction();
    // ... create transaction record
}
```

**Safety Mechanisms**:
1. **Pessimistic Lock**: Wallet is locked before any operation
2. **Balance Check**: Verified in locked state (cannot change mid-check)
3. **Atomic Update**: Both balance and transaction log updated in same transaction
4. **Automatic Rollback**: If any step fails, entire debit is reversed

**Conflict Scenario**:
```
User has $100

Order A (Cost: $60)            Order B (Cost $60)
├─ Lock Wallet
├─ Read Balance: $100
├─ Check: $100 >= $60 ✓
├─ Debit: $100 - $60 = $40
├─ Save: Balance = $40
├─ Unlock
                               ├─ Lock Wallet (waiting...)
                               ├─ Read Balance: $40
                               ├─ Check: $40 >= $60 ✗ → REJECT (InsufficientFundsException)

Result: Order A succeeds, Order B fails with clear error
```

---

## 5. Query Optimization (N+1 Prevention)

### Eager Loading with JOIN FETCH
**Location**: `OrderServiceImpl.toOrderDetailResponse()` (lines 196-203)

```java
// fetch all order items for this order with menuItem eagerly loaded via join
List<OrderItem> orderItems = orderItemRepository.findByOrder_IdWithMenuItems(orderId);

// Query from OrderItemRepository:
@Query("SELECT oi FROM OrderItem oi LEFT JOIN FETCH oi.menuItem WHERE oi.order.id = :orderId")
List<OrderItem> findByOrder_IdWithMenuItems(@Param("orderId") Long orderId);

// fetch vendor status rows and index by orderItem id for O(1) lookup
Map<Long, VendorOrderStatusType> statusByOrderItemId = new HashMap<>();
for (VendorOrderStatus vendorStatus : vendorOrderStatusRepository.findByOrderItem_Order_IdWithOrderItem(orderId)) {
    statusByOrderItemId.put(vendorStatus.getOrderItem().getId(), vendorStatus.getStatus());
}
```

**Benefits**:
- **Single query** fetches OrderItem + MenuItem data (no N+1 problem)
- **HashMap index** enables O(1) vendor status lookups
- **Reduces database round-trips** under concurrent load

---

## 6. Order Timeout Handling

### Safe Timeout Processing
**Location**: `OrderServiceImpl.processTimeout()` (lines 233-269)

```java
@Override
@Transactional
public OrderDetailResponse processTimeout(Long userId, Long orderId) {
    Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
    
    // Ownership check — reject if order belongs to a different user
    if (!order.getUserId().equals(userId)) {
        throw new SecurityException("Access denied: order does not belong to this user.");
    }
    
    LocalDateTime now = LocalDateTime.now();
    List<VendorOrderStatus> statuses = vendorOrderStatusRepository.findByOrderItem_Order_IdWithOrderItem(orderId);
    
    boolean timeoutApplied = false;
    for (VendorOrderStatus status : statuses) {
        boolean isPending = status.getStatus() == VendorOrderStatusType.PENDING;
        boolean isExpired = status.getActionExpiresAt() != null && !status.getActionExpiresAt().isAfter(now);
        if (isPending && isExpired) {
            status.setStatus(VendorOrderStatusType.TIMEOUT);
            status.setActionedAt(now);
            timeoutApplied = true;
        }
    }
    
    if (timeoutApplied) {
        vendorOrderStatusRepository.saveAll(statuses);
        if (!"REFUNDED".equalsIgnoreCase(order.getPaymentStatus())) {
            walletService.refund(userId, order.getTotalAmount(), "Order timeout refund");
        }
        order.setOverallStatus("TIMEOUT");
        order.setPaymentStatus("REFUNDED");
        orderRepository.save(order);
    }
}
```

**Safety Features**:
1. **Ownership verification**: Only the user who placed the order can trigger timeout
2. **Atomic refund**: `walletService.refund()` is transactional (includes wallet lock)
3. **Idempotent refund check**: `if (!"REFUNDED".equalsIgnoreCase(...))` prevents duplicate refunds
4. **Status consistency**: All vendor item statuses and main order status updated together

---

## 7. Configuration for Concurrent Load

### Spring Transaction Manager
The application uses Spring Data JPA with default transaction management:
- **Isolation Level**: READ_COMMITTED (MySQL default for InnoDB)
- **Lock Timeout**: Database default (typically 50 seconds for MySQL)
- **Propagation**: REQUIRED (reuses transaction if exists)

### Database Configuration
**Location**: `application.yml` (lines 5-17)
```yaml
datasource:
  url: jdbc:mysql://localhost:3306/cafetron
  driver-class-name: com.mysql.cj.jdbc.Driver
jpa:
  hibernate:
    ddl-auto: update
  properties:
    hibernate:
      format_sql: true
```

---

## 8. Conflict Resolution Summary Table

| Conflict Type | Prevention Method | Mechanism |
|---------------|-------------------|-----------|
| **Stock Race Condition** | Pessimistic Lock | PESSIMISTIC_WRITE on MenuItem |
| **Overshooting Wallet Balance** | Pessimistic Lock + Check | Lock wallet, verify balance in locked state |
| **Deadlock (multiple items)** | Sorted Lock Order | Items locked in ascending ID order |
| **Duplicate Refunds** | Idempotent Check | Check `paymentStatus` before refunding |
| **Unauthorized Access** | Ownership Check | Verify `userId` matches order creator |
| **Data Inconsistency** | ACID Transactions | All-or-nothing order placement |
| **N+1 Query Problems** | JOIN FETCH | Eager load MenuItems with OrderItems |

---

## 9. Thread-Safe Order Flow

```
User A Places Order               User B Places Order
├─ Lock MenuItem[50]
├─ Check: Stock >= Qty
├─ Deduct Stock
├─ Lock Wallet[UserA]
├─ Check: Balance >= Amount
├─ Debit Wallet
├─ Create Order Record
├─ Create OrderItems
├─ Create VendorOrderStatus
├─ Generate QR Token
├─ Unlock All
                                  ├─ Lock MenuItem[50] (waiting if User A has it)
                                  ├─ ... (same steps)
                                  
Result: Both orders safely processed in serialized sequence
or one fails entirely (no partial orders)
```

---

## 10. Key Takeaways

1. **Pessimistic Locking** at database level (not optimistic) → Safe for high contention
2. **Sorted Lock Order** → Prevents deadlocks when multiple items involved
3. **ACID Transactions** → All-or-nothing semantics per order placement
4. **Ownership Checks** → Prevents unauthorized access to other users' orders
5. **Idempotent Operations** → Refunds won't double-charge even if retried
6. **Query Optimization** → Prevents cascading lock contention under load

---

## Testing the Concurrency Safety

To verify the system handles conflicts correctly, consider these test scenarios:

1. **Concurrent Stock Depletion**: Two users order the last 5 items (qty 3 and 3)
   - Expected: One succeeds, one fails with stock error
   
2. **Insufficient Wallet Balance**: User with $50 balance places two $40 orders simultaneously
   - Expected: First succeeds, second fails with InsufficientFundsException
   
3. **Timeout Race**: User and system both attempt to process timeout simultaneously
   - Expected: Refund happens exactly once, status updates atomically
   
4. **Concurrent Refunds**: User requests cancel + system timeout both trigger
   - Expected: One refund processed, second detects REFUNDED status and skips


