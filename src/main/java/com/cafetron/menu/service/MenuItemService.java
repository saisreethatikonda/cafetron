package com.cafetron.menu.service;

import com.cafetron.menu.dto.MenuItemRequest;
import com.cafetron.menu.dto.MenuItemResponse;
import com.cafetron.menu.entity.MenuItem;
import com.cafetron.menu.repository.MenuItemRepository;
import com.cafetron.security.UserPrincipal;
import com.cafetron.vendor.entity.Vendor;
import com.cafetron.vendor.repository.VendorRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class MenuItemService {
    private static final String ROLE_COUNTER = "COUNTER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final MenuItemRepository menuItemRepository;
    private final VendorRepository vendorRepository;

    public MenuItemService(MenuItemRepository menuItemRepository,
                           VendorRepository vendorRepository) {
        this.menuItemRepository = menuItemRepository;
        this.vendorRepository = vendorRepository;
    }

    public MenuItemResponse create(UserPrincipal principal, MenuItemRequest request) {
        requireCounterOrAdmin(principal);

        Vendor vendor = getActiveVendor(request.vendorId());

        MenuItem item = new MenuItem();
        item.setItemName(request.itemName());
        item.setPrice(request.price());
        item.setStock(request.stock());
        item.setFoodType(request.foodType());
        item.setVendor(vendor);
        item.setAvailable(request.stock() > 0);

        return toResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse getById(UserPrincipal principal, Long menuItemId) {
        requireAuthenticated(principal);

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        return toResponse(item);
    }

    public List<MenuItemResponse> getAll(UserPrincipal principal) {
        requireCounterOrAdmin(principal);

        return menuItemRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse update(UserPrincipal principal, Long menuItemId, MenuItemRequest request) {
        requireCounterOrAdmin(principal);

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));

        Vendor vendor = getActiveVendor(request.vendorId());

        item.setItemName(request.itemName());
        item.setPrice(request.price());
        item.setStock(request.stock());
        item.setFoodType(request.foodType());
        item.setVendor(vendor);
        item.setAvailable(request.stock() > 0);

        return toResponse(menuItemRepository.save(item));
    }

    public void delete(UserPrincipal principal, Long menuItemId) {
        requireCounterOrAdmin(principal);
        menuItemRepository.deleteById(menuItemId);
    }

    public List<MenuItemResponse> getTodaysMenu(UserPrincipal principal) {
        requireAuthenticated(principal);

        return menuItemRepository.findTodaysMenu()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MenuItemResponse> search(UserPrincipal principal, String name) {
        requireAuthenticated(principal);

        return menuItemRepository.searchTodaysMenu(name)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MenuItemResponse> filterByFoodType(UserPrincipal principal, String foodType) {
        requireAuthenticated(principal);

        return menuItemRepository.filterByFoodType(foodType)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public MenuItemResponse setStock(UserPrincipal principal, Long menuItemId, int newStock) {
        requireCounterOrAdmin(principal);

        if (newStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Stock cannot be negative");
        }

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        item.setStock(newStock);
        item.setAvailable(newStock > 0);
        return toResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse setAvailability(UserPrincipal principal, Long menuItemId, boolean available) {
        requireCounterOrAdmin(principal);

        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        if (available && item.getStock() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot make an item available with zero stock");
        }
        item.setAvailable(available);
        return toResponse(menuItemRepository.save(item));
    }

    public MenuItemResponse decreaseStock(Long menuItemId, int quantity) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Menu item not found"));
        if (item.getStock() < quantity) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Not enough stock");
        }
        item.setStock(item.getStock() - quantity);
        item.setAvailable(item.getStock() > 0);
        return toResponse(menuItemRepository.save(item));
    }

    private Vendor getActiveVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        if (!vendor.isActive()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot assign menu items to an inactive vendor");
        }
        return vendor;
    }

    private MenuItemResponse toResponse(MenuItem item) {
        return new MenuItemResponse(
                item.getId(),
                item.getItemName(),
                item.getPrice(),
                item.getStock(),
                item.getFoodType(),
                item.isAvailable(),
                item.getVendor().getId(),
                item.getVendor().getName()
        );
    }

    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }

    private void requireCounterOrAdmin(UserPrincipal principal) {
        requireAuthenticated(principal);
        String role = principal.getRole() == null
                ? ""
                : principal.getRole().trim().toUpperCase(Locale.ROOT);
        if (!ROLE_COUNTER.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }
}
