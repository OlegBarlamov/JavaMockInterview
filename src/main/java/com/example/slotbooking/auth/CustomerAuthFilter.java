package com.example.slotbooking.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Stand-in for the upstream auth filter. The API gateway authenticates the customer and forwards the
 * customer id in a trusted header; this filter exposes it as the request {@link Principal}.
 */
@Component
public class CustomerAuthFilter extends OncePerRequestFilter {

    static final String CUSTOMER_ID_HEADER = "X-Authenticated-Customer-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String customerId = request.getHeader(CUSTOMER_ID_HEADER);
        if (customerId == null || customerId.isBlank()) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        CustomerPrincipal principal = new CustomerPrincipal(customerId);
        chain.doFilter(new HttpServletRequestWrapper(request) {
            @Override
            public Principal getUserPrincipal() {
                return principal;
            }
        }, response);
    }
}
