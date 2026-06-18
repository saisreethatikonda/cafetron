package com.cafetron.admin;

import com.cafetron.cart.entity.OrderItem;
import com.cafetron.cart.repository.OrderItemRepository;
import com.cafetron.menu.entity.MenuItem;
import com.cafetron.menu.repository.MenuItemRepository;
import com.cafetron.vendor.repository.VendorRepository;
import com.cafetron.order.entity.Order;
import com.cafetron.order.repository.OrderRepository;
import com.cafetron.orderQR.entity.OrderQR;
import com.cafetron.orderQR.repository.OrderQRRepository;
import com.cafetron.pickup.VendorOrderStatus;
import com.cafetron.pickup.VendorOrderStatusType;
import com.cafetron.pickup.repository.VendorOrderStatusRepository;
import com.cafetron.security.JwtUtil;
import com.cafetron.security.UserPrincipal;
import com.cafetron.user.User;
import com.cafetron.user.repository.UserRepository;
import com.cafetron.vendor.entity.Vendor;
import com.cafetron.wallet.entity.Transaction;
import com.cafetron.wallet.entity.TransactionType;
import com.cafetron.wallet.entity.Wallet;
import com.cafetron.wallet.repository.TransactionRepository;
import com.cafetron.wallet.repository.WalletRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dev")
@ConditionalOnProperty(name = "cafetron.dev-seed.enabled", havingValue = "true")
public class DevSeedController {

    private static final String TEST_EMPLOYEE_ID = "E2E1001";
    private static final String TEST_PASSWORD = "Test@12345";
    private static final String TEST_EMAIL = "e2e.employee@cafetron.local";
    private static final String TEST_VENDOR_EMAIL = "e2e.vendor@cafetron.local";

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final VendorRepository vendorRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final VendorOrderStatusRepository vendorOrderStatusRepository;
    private final OrderQRRepository orderQRRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public DevSeedController(UserRepository userRepository,
                             WalletRepository walletRepository,
                             TransactionRepository transactionRepository,
                             VendorRepository vendorRepository,
                             MenuItemRepository menuItemRepository,
                             OrderRepository orderRepository,
                             OrderItemRepository orderItemRepository,
                             VendorOrderStatusRepository vendorOrderStatusRepository,
                             OrderQRRepository orderQRRepository,
                             PasswordEncoder passwordEncoder,
                             JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.transactionRepository = transactionRepository;
        this.vendorRepository = vendorRepository;
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.vendorOrderStatusRepository = vendorOrderStatusRepository;
        this.orderQRRepository = orderQRRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<Map<String, Object>> seedAllTables(@AuthenticationPrincipal UserPrincipal principal) {
        LocalDateTime now = LocalDateTime.now();

        User user = userRepository.findByEmployeeId(TEST_EMPLOYEE_ID)
                .orElseGet(() -> {
                    User u = new User();
                    u.setName("E2E Test Employee");
                    u.setEmail(TEST_EMAIL);
                    u.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
                    u.setEmployeeId(TEST_EMPLOYEE_ID);
                    u.setDepartment("QA");
                    u.setRole("EMPLOYEE");
                    u.setCreatedAt(now);
                    return userRepository.save(u);
                });

        Wallet wallet = walletRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setUser(user);
                    w.setBalance(new BigDecimal("1500.00"));
                    w.setUpdatedAt(now);
                    return walletRepository.save(w);
                });

        // Fix existing wallets seeded before @Version default was set
        if (wallet.getVersion() == null) {
            wallet.setVersion(0L);
            wallet = walletRepository.save(wallet);
        }

        if (transactionRepository.findByWallet_IdOrderByCreatedAtDesc(wallet.getId()).isEmpty()) {
            Transaction topUp = new Transaction();
            topUp.setWallet(wallet);
            topUp.setAmount(new BigDecimal("1500.00"));
            topUp.setType(TransactionType.TOP_UP);
            topUp.setDescription("Initial E2E top-up");
            transactionRepository.save(topUp);
        }

        Vendor vendor = vendorRepository.findByEmail(TEST_VENDOR_EMAIL)
                .orElseGet(() -> {
                    Vendor v = new Vendor();
                    v.setName("E2E Vendor");
                    v.setEmail(TEST_VENDOR_EMAIL);
                    v.setPhone("9999999999");
                    v.setContactPerson("E2E Counter");
                    v.setActive(true);
                    return vendorRepository.save(v);
                });

        List<MenuItem> vendorItems = menuItemRepository.findByVendorId(vendor.getId());
        MenuItem menuItem;
        if (vendorItems.isEmpty()) {
            MenuItem item = new MenuItem();
            item.setVendor(vendor);
            item.setItemName("E2E Paneer Bowl");
            item.setPrice(120.0);
            item.setStock(30);
            item.setFoodType("VEG");
            item.setAvailable(true);
            menuItem = menuItemRepository.save(item);
        } else {
            menuItem = vendorItems.get(0);
        }

        Order order = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    Order o = new Order();
                    o.setUserId(user.getId());
                    o.setToken("E2E-TOKEN-" + System.currentTimeMillis());
                    o.setOverallStatus("PLACED");
                    o.setPaymentStatus("PAID");
                    o.setLocation("Cafeteria Block A");
                    o.setPickupSlot("13:00-13:30");
                    o.setTotalAmount(new BigDecimal("240.00"));
                    o.setVendorCount(1);
                    o.setVendorAcceptedCount(0);
                    o.setCreatedAt(now);
                    return orderRepository.save(o);
                });

        OrderItem orderItem = orderItemRepository.findByOrder_Id(order.getId())
                .stream()
                .findFirst()
                .orElseGet(() -> {
                    OrderItem oi = new OrderItem();
                    oi.setOrder(order);
                    oi.setMenuItem(menuItem);
                    oi.setQuantity(2);
                    oi.setUnitPrice(new BigDecimal("120.00"));
                    return orderItemRepository.save(oi);
                });

        if (vendorOrderStatusRepository.findByOrderItem_Order_Id(order.getId()).isEmpty()) {
            VendorOrderStatus vos = new VendorOrderStatus();
            vos.setOrderItem(orderItem);
            vos.setVendor(vendor);
            vos.setStatus(VendorOrderStatusType.PENDING);
            vos.setActionExpiresAt(now.plusMinutes(30));
            vos.setCreatedAt(now);
            vendorOrderStatusRepository.save(vos);
        }

        if (orderQRRepository.findAll().stream().noneMatch(q -> q.getOrder().getId().equals(order.getId()))) {
            OrderQR qr = new OrderQR();
            qr.setOrder(order);
            qr.setQrData("E2E-QR-" + order.getId());
            qr.setCreatedAt(now);
            orderQRRepository.save(qr);
        }

        if (!transactionRepository.existsByOrder_IdAndType(order.getId(), TransactionType.DEBIT)) {
            Transaction debit = new Transaction();
            debit.setWallet(wallet);
            debit.setOrder(order);
            debit.setAmount(new BigDecimal("240.00"));
            debit.setType(TransactionType.DEBIT);
            debit.setDescription("E2E seeded order debit");
            transactionRepository.save(debit);
        }

        String token = jwtUtil.generateToken(new UserPrincipal(user));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("employeeId", TEST_EMPLOYEE_ID);
        response.put("password", TEST_PASSWORD);
        response.put("jwt", token);
        response.put("userId", user.getId());
        response.put("walletId", wallet.getId());
        response.put("vendorId", vendor.getId());
        response.put("menuItemId", menuItem.getId());
        response.put("orderId", order.getId());
        response.put("message", "Seeded user, wallet, transaction, vendor, menu_item, order, order_item, vendor_order_status, orderqr");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}


