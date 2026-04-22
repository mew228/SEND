package dev.send.api.auth;

import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserAccessor {
    public Optional<CurrentUser> findCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof Jwt jwt)) {
            return Optional.empty();
        }

        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(new CurrentUser(subject, jwt.getClaimAsString("email")));
    }

    public CurrentUser requireCurrentUser() {
        return findCurrentUser()
                .orElseThrow(() -> new AccessDeniedException("Authentication is required."));
    }
}
