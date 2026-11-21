package es.uclm.esi.iso2.common.access.domain;

import java.util.List;

/**
 * Represents a user account in the system.
 */
public class UserAccount {
    private final String username;
    private final String fullName;
    private final String email;
    private final boolean active;
    private final List<Role> roles;

    /**
     * Creates a user account.
     *
     * @param username the username
     * @param fullName the full name
     * @param email the email address
     * @param active whether the account is active
     * @param roles the roles assigned to the user
     */
    public UserAccount(String username, String fullName, String email, boolean active, List<Role> roles) {
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
        this.roles = List.copyOf(roles);
    }

    /**
     * Returns the username of the account.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the full name of the user.
     *
     * @return the full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Returns the email address of the user.
     *
     * @return the email
     */
    public String getEmail() {
        return email;
    }

    /**
     * Tells whether the user account is active.
     *
     * @return {@code true} if the account is active, {@code false} otherwise
     */
    public boolean isActive() {
        return active;
    }

    /**
     * Returns the roles assigned to the user.
     *
     * @return an unmodifiable list of roles
     */
    public List<Role> getRoles() {
        return roles;
    }
}
