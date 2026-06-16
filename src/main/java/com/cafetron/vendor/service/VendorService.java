package com.cafetron.vendor.service;
import com.cafetron.security.UserPrincipal;
import com.cafetron.vendor.dto.VendorRequest;
import com.cafetron.vendor.dto.VendorResponse;
import com.cafetron.vendor.entity.Vendor;
import com.cafetron.vendor.repository.VendorRepository;
import jakarta.transaction.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
@Service
@Transactional
public class VendorService {
    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_VENDOR = "VENDOR";
    private final VendorRepository vendorRepository;
    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }
    public VendorResponse create(UserPrincipal principal, VendorRequest request) {
        requireAdmin(principal);
        if (vendorRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor email already exists");
        }
        Vendor vendor = new Vendor();
        vendor.setName(request.name());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setContactPerson(request.contactPerson());
        vendor.setActive(true);
        vendor.setCreatedAt(LocalDateTime.now());
        return toResponse(vendorRepository.save(vendor));
    }
    public VendorResponse getById(UserPrincipal principal, Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        requireAdminOrSameVendor(principal, vendor);
        return toResponse(vendor);
    }
    public List<VendorResponse> getAll(UserPrincipal principal) {
        requireVendorOrAdmin(principal);
        if (isVendor(principal)) {
            return List.of(toResponse(getVendorForPrincipal(principal)));
        }
        return vendorRepository.findAll().stream().map(this::toResponse).toList();
    }
    public List<VendorResponse> getActive(UserPrincipal principal) {
        requireVendorOrAdmin(principal);
        if (isVendor(principal)) {
            Vendor vendor = getVendorForPrincipal(principal);
            return vendor.isActive() ? List.of(toResponse(vendor)) : List.of();
        }
        return vendorRepository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }
    public VendorResponse update(UserPrincipal principal, Long vendorId, VendorRequest request) {
        requireAdmin(principal);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        if (!vendor.getEmail().equalsIgnoreCase(request.email()) && vendorRepository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Vendor email already exists");
        }
        vendor.setName(request.name());
        vendor.setEmail(request.email());
        vendor.setPhone(request.phone());
        vendor.setContactPerson(request.contactPerson());
        return toResponse(vendorRepository.save(vendor));
    }
    public VendorResponse setActive(UserPrincipal principal, Long vendorId, boolean active) {
        requireAdmin(principal);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        vendor.setActive(active);
        return toResponse(vendorRepository.save(vendor));
    }
    public void delete(UserPrincipal principal, Long id) {
        requireAdmin(principal);
        if (!vendorRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found");
        }
        vendorRepository.deleteById(id);
    }
    private VendorResponse toResponse(Vendor v) {
        return new VendorResponse(
                v.getId(), v.getName(), v.getEmail(), v.getPhone(),
                v.getContactPerson(), v.isActive(), v.getCreatedAt()
        );
    }
    private void requireAuthenticated(UserPrincipal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }
    }
    private void requireAdmin(UserPrincipal principal) {
        requireAuthenticated(principal);
        if (!ROLE_ADMIN.equals(normalizedRole(principal))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }
    private void requireVendorOrAdmin(UserPrincipal principal) {
        requireAuthenticated(principal);
        String role = normalizedRole(principal);
        if (!ROLE_VENDOR.equals(role) && !ROLE_ADMIN.equals(role)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
        }
    }
    private void requireAdminOrSameVendor(UserPrincipal principal, Vendor vendor) {
        requireAuthenticated(principal);
        if (isAdmin(principal)) {
            return;
        }
        if (isVendor(principal) && principal.getUser().getEmail() != null
                && principal.getUser().getEmail().equalsIgnoreCase(vendor.getEmail())) {
            return;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to perform this action");
    }
    private Vendor getVendorForPrincipal(UserPrincipal principal) {
        String email = principal.getUser().getEmail();
        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vendor account is missing an email");
        }
        return vendorRepository.findByEmail(email)
                .orElseGet(() -> createVendorProfile(principal));
    }
    private boolean isAdmin(UserPrincipal principal) {
        return ROLE_ADMIN.equals(normalizedRole(principal));
    }
    private boolean isVendor(UserPrincipal principal) {
        return principal != null && ROLE_VENDOR.equals(normalizedRole(principal));
    }
    private String normalizedRole(UserPrincipal principal) {
        return principal.getRole() == null
                ? ""
                : principal.getRole().trim().replaceFirst("^ROLE_", "").toUpperCase(Locale.ROOT);
    }

    private Vendor createVendorProfile(UserPrincipal principal) {
        Vendor vendor = new Vendor();
        vendor.setName(principal.getUser().getName());
        vendor.setEmail(principal.getUser().getEmail());
        vendor.setContactPerson(principal.getUser().getName());
        vendor.setActive(true);
        vendor.setCreatedAt(LocalDateTime.now());
        return vendorRepository.save(vendor);
    }
}
