package com.cafetron.menu.controller;

import com.cafetron.menu.dto.MenuItemRequest;
import com.cafetron.menu.dto.MenuItemResponse;
import com.cafetron.menu.service.MenuItemService;
import com.cafetron.security.UserPrincipal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4200")   // allow the Angular dev app
@RestController
@RequestMapping({"/menu", "/api/menu"})
public class MenuItemController {
    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    // POST /menu -> create a new item.
    @PostMapping
    public MenuItemResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                   @Valid @RequestBody MenuItemRequest request) {
        return menuItemService.create(principal, request);
    }

    // GET /menu -> all items (staff/admin full inventory).
    @GetMapping
    public List<MenuItemResponse> getAll(@AuthenticationPrincipal UserPrincipal principal) {
        return menuItemService.getAll(principal);
    }

    // GET /menu/{id} -> one item.
    @GetMapping("/today")
    public List<MenuItemResponse> getTodaysMenu(@AuthenticationPrincipal UserPrincipal principal) {
        return menuItemService.getTodaysMenu(principal);
    }

    // PUT /menu/{id} -> replace/update an existing item.
    @PutMapping("/{id}")
    public MenuItemResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                   @PathVariable Long id,
                                   @Valid @RequestBody MenuItemRequest request) {
        return menuItemService.update(principal, id, request);
    }

    // DELETE /menu/{id} -> remove an item.
    @DeleteMapping("/{id}")
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        menuItemService.delete(principal, id);
    }

    // --- Employee-facing reads ---

    // GET /menu/today -> available items from active vendors.
    @GetMapping("/search")
    public List<MenuItemResponse> search(@AuthenticationPrincipal UserPrincipal principal,
                                         @RequestParam String name) {
        return menuItemService.search(principal, name);
    }

    // GET /menu/search?name=dosa -> search today's menu by name.
    @GetMapping("/filter")
    public List<MenuItemResponse> filterByFoodType(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestParam String type) {
        return menuItemService.filterByFoodType(principal, type);
    }

    // GET /menu/filter?type=VEG -> filter today's menu by dietary type.
    @GetMapping("/{id}")
    public MenuItemResponse getById(@AuthenticationPrincipal UserPrincipal principal,
                                    @PathVariable Long id) {
        return menuItemService.getById(principal, id);
    }

    // --- Stock operations ---

    // PATCH /menu/{id}/stock?stock=30 -> set the stock level.
    // PATCH = update just part of an item (here, only the stock).
    @PatchMapping("/{id}/stock")
    public MenuItemResponse setStock(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable Long id,
                                     @RequestParam int stock) {
        return menuItemService.setStock(principal, id, stock);
    }

    // PATCH /menu/{id}/availability?available=false -> manually show/hide.
    @PatchMapping("/{id}/availability")
    public MenuItemResponse setAvailability(@AuthenticationPrincipal UserPrincipal principal,
                                            @PathVariable Long id,
                                            @RequestParam boolean available) {
        return menuItemService.setAvailability(principal, id, available);
    }
}

