package com.ecommerce.auth.domain.model;

/**
 * Application roles for RBAC. Mapped to Spring Security authorities as
 * {@code ROLE_<name>}.
 */
public enum Role {
    ADMIN,
    CUSTOMER;

    /** Spring Security authority representation. */
    public String authority() {
        return "ROLE_" + name();
    }
}
