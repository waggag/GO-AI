package com.goaiplatform.backend.security;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

public class JwtUserAuthentication extends UsernamePasswordAuthenticationToken {

    private final Long userId;

    public JwtUserAuthentication(Long userId, String principal, String credentials,
                                  Collection<? extends GrantedAuthority> authorities) {
        super(principal, credentials, authorities);
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }
}
