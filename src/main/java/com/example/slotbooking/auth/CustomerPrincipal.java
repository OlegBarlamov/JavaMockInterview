package com.example.slotbooking.auth;

import java.security.Principal;

/**
 * The authenticated customer. Controllers can declare a {@code CustomerPrincipal} parameter to receive it.
 */
public record CustomerPrincipal(String customerId) implements Principal {

    @Override
    public String getName() {
        return customerId;
    }
}
