package com.clearing.netting.adapter.in.web.auth;

import com.clearing.netting.adapter.in.web.dto.ErrorResponse;
import com.clearing.netting.application.TokenService;
import com.clearing.netting.domain.exception.DomainException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final Set<String> PUBLIC_PATHS = Set.of(
            "/api/health",
            "/api/auth/login"
    );

    private final TokenService tokenService;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(TokenService tokenService, ObjectMapper objectMapper) {
        this.tokenService = tokenService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String header = request.getHeader("Authorization");
            if (header == null || !header.startsWith("Bearer ")) {
                writeUnauthorized(response, "missing bearer token");
                return;
            }
            TokenService.TokenPayload payload = tokenService.parse(header.substring(7));
            AuthContext.set(new AuthUser(payload.username(), payload.role()));
        } catch (DomainException ex) {
            writeUnauthorized(response, ex.getMessage());
            return;
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), ErrorResponse.of("UNAUTHORIZED", message));
    }

    private boolean isPublic(String path) {
        return PUBLIC_PATHS.contains(path);
    }
}
