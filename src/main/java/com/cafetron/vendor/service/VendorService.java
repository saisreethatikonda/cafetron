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
    private static final String ROLE_COUNTER = "COUNTER";
    private static final String ROLE_ADMIN = "ADMIN";
    private final VendorRepository vendorRepository;
    public VendorService(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }
    public VendorResponse create(UserPrincipal principal, VendorRequest request) {
        requireCounterOrAdmin(principal);
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
        requireCounterOrAdmin(principal);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        return toResponse(vendor);
    }
    public List<VendorResponse> getAll(UserPrincipal principal) {
        requireCounterOrAdmin(principal);
        return vendorRepository.findAll().stream().map(this::toResponse).toList();
    }
    public List<VendorResponse> getActive(UserPrincipal principal) {
        requireCounterOrAdmin(principal);
        return vendorRepository.findByIsActiveTrue().stream().map(this::toResponse).toList();
    }
    public VendorResponse update(UserPrincipal principal, Long vendorId, VendorRequest request) {
        requireCounterOrAdmin(principal);
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
        requireCounterOrAdmin(principal);
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vendor not found"));
        vendor.setActive(active);
        return toResponse(vendorRepository.save(vendor));
    }
    public void delete(UserPrincipal principal, Long id) {
        requireCounterOrAdmin(principal);
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
