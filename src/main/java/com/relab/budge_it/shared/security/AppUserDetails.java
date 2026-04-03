package com.relab.budge_it.shared.security;

import com.relab.budge_it.identity.domain.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AppUserDetails implements UserDetails {

    @Getter
    private final UUID userId;

    private final String email;
    private final String passwordHash;
    private final User.UserStatus status;

    public AppUserDetails(User user) {
        this.userId       = user.getId();
        this.email        = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.status       = user.getStatus();
    }

    // ─── UserDetails contract ─────────────────────────────────────────────────

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    // No role-based access control yet — all authenticated users have the same access
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return status != User.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return status == User.UserStatus.ACTIVE;
    }
}