package com.innowise.academy.orderservice.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public record CurrentUser(Long userId, boolean admin, String token) {

    public static CurrentUser from(JwtAuthenticationToken authenticationToken){
        String userIdFromToken = authenticationToken.getToken().getClaimAsString("user_id");

        if(userIdFromToken == null){
            throw new AccessDeniedException("Current token hasn't userId");
        }

        boolean admin = authenticationToken.getAuthorities()
                .stream()
                .anyMatch(aut -> "ROLE_ADMIN".equals(aut.getAuthority()));

        return new CurrentUser(java.lang.Long.valueOf(userIdFromToken),
                admin,
                authenticationToken.getToken().getTokenValue());
    }

    @Override
    public String toString() {
        return "CurrentUser{" +
                "userId=" + userId +
                ", admin=" + admin +
                '}';
    }
}
