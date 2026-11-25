package es.uclm.esi.iso2.common.access.domain;

import java.util.List;

/**
 * Represents a user role with its associated permissions (UC18).
 */
public class Role {
    private final String id;
    private final String name;
    private final String description;
    private final List<Permission> permissions;

    /**
     * Creates a role with no permissions.
     *
     * @param id the unique identifier of the role
     * @param name the name of the role
     * @param description the description of the role
     */
    public Role(String id, String name, String description) {
        this(id, name, description, List.of());
    }

    /**
     * Creates a role with the given permissions.
     *
     * @param id the unique identifier of the role
     * @param name the name of the role
     * @param description the description of the role
     * @param permissions the permissions granted to this role
     */
    public Role(String id, String name, String description, List<Permission> permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = List.copyOf(permissions);
    }

    /**
     * Returns the unique identifier of the role.
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the role.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the description of the role.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the permissions granted to this role.
     *
     * @return an unmodifiable list of permissions
     */
    public List<Permission> getPermissions() {
        return permissions;
    }
}
