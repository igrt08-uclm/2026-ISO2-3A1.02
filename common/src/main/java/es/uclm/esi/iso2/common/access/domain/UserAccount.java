package es.uclm.esi.iso2.common.access.domain;

import java.util.List;

public class UserAccount {
    private final String username;
    private final String fullName;
    private final String email;
    private final boolean active;
    private final List<Role> roles;

    public UserAccount(String username, String fullName, String email, boolean active, List<Role> roles) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.roles = List.copyOf(roles);
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public boolean isActive() {
        return active;
    }

    public List<Role> getRoles() {
        return roles;
    }
}
