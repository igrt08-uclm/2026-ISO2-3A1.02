package es.uclm.esi.iso2.common.access.domain;

public class Role {
    private final String id;
    private final String name;
    private final String description;

    public Role(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
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
}
