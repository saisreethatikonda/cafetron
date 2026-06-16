package com.cafetron.vendor.controller;

import com.cafetron.security.UserPrincipal;
import com.cafetron.vendor.dto.VendorRequest;
import com.cafetron.vendor.dto.VendorResponse;
import com.cafetron.vendor.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

    @RestController
    @RequestMapping({"/vendors", "/api/vendors"})
    public class VendorController {

        private final VendorService vendorService;

        public VendorController(VendorService vendorService) {
            this.vendorService = vendorService;
        }

        @PostMapping                                       // POST /vendors
        public VendorResponse create(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody VendorRequest request) {
            return vendorService.create(principal, request);
        }

        @GetMapping                                        // GET /vendors (all)
        public List<VendorResponse> getAll(@AuthenticationPrincipal UserPrincipal principal) {
            return vendorService.getAll(principal);
        }

        @GetMapping("/active")                             // GET /vendors/active
        public List<VendorResponse> getActive(@AuthenticationPrincipal UserPrincipal principal) {
            return vendorService.getActive(principal);
        }

        @GetMapping("/{id}")                               // GET /vendors/5
        public VendorResponse getById(@AuthenticationPrincipal UserPrincipal principal,
                                      @PathVariable("id") Long vendorId) {
            return vendorService.getById(principal, vendorId);
        }

        @PutMapping("/{id}")                               // PUT /vendors/5
        public VendorResponse update(@AuthenticationPrincipal UserPrincipal principal,
                                     @PathVariable("id") Long vendorId,
                                     @Valid @RequestBody VendorRequest request) {
            return vendorService.update(principal, vendorId, request);
        }

        @PatchMapping("/{id}/active")                      // PATCH /vendors/5/active?active=false
        public VendorResponse setActive(@AuthenticationPrincipal UserPrincipal principal,
                                        @PathVariable("id") Long vendorId,
                                        @RequestParam boolean active) {
            return vendorService.setActive(principal, vendorId, active);
        }

        @DeleteMapping("/{id}")                            // DELETE /vendors/5
        public void delete(@AuthenticationPrincipal UserPrincipal principal,
                           @PathVariable("id") Long vendorId) {
            vendorService.delete(principal, vendorId);
        }
}
