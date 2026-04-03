package com.relab.budge_it.shared.security;

import com.relab.budge_it.shared.response.BusinessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class AuthenticatedUserProvider {

    /**
     * Returns the UUID of the currently authenticated user.
     * Throws 401 if called outside of an authenticated request context.
     */
    public UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw BusinessException.unauthorized("No authenticated user found.");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof AppUserDetails appUser) {
            return appUser.getUserId();
        }

        throw BusinessException.unauthorized("Unable to resolve authenticated user.");
    }

    /**
     * Returns the full AppUserDetails of the authenticated user.
     * Use this when you need more than just the ID.
     */
    public AppUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null || !auth.isAuthenticated()) {
            throw BusinessException.unauthorized("No authenticated user found.");
        }

        if (auth.getPrincipal() instanceof AppUserDetails appUser) {
            return appUser;
        }

        throw BusinessException.unauthorized("Unable to resolve authenticated user.");
    }
}