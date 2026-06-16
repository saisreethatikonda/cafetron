package com.cafetron.security;

import com.cafetron.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class UserPrincipal implements UserDetails {

    private final User user;

    public UserPrincipal(User user) {
        this.user = user;
    }

    // ── Expose the underlying User object ─────────────────────────

    public User getUser() {
        return user;
    }

    public Long getId() {
        return user.getId();
    }

    public String getRole() {
        return normalizeRole(user.getRole());
    }

    // ── UserDetails contract ──────────────────────────────────────

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(
                new SimpleGrantedAuthority("ROLE_" + getRole())
        );
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return String.valueOf(user.getId());  // ← user.id is the internal identifier
    }

    @Override
    public boolean isAccountNonExpired()     { return true; }

    @Override
    public boolean isAccountNonLocked()      { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled()               { return true; }

    private String normalizeRole(String role) {
        if (role == null || role.isBlank()) {
            return "";
        }

        String normalizedRole = role.trim().replaceFirst("^ROLE_", "").toUpperCase();
        return "COUNTER".equals(normalizedRole) ? "VENDOR" : normalizedRole;
    }
}
