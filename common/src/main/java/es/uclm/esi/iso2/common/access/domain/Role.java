package es.uclm.esi.iso2.common.access.domain;

import java.util.List;

public class Role {
    private final String id;
    private final String name;
    private final String description;
    private final List<Permission> permissions;

    public Role(String id, String name, String description) {
        this(id, name, description, List.of());
    }

    public Role(String id, String name, String description, List<Permission> permissions) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.permissions = List.copyOf(permissions);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<Permission> getPermissions() {
        return permissions;
    }
}
