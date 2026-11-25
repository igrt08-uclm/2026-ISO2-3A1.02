package es.uclm.esi.iso2.common.access.domain;

/**
 * Represents a permission that can be granted to roles (UC18).
 */
public class Permission {
    private final String code;
    private final String description;

    /**
     * Creates a permission.
     *
     * @param code the unique code of the permission
     * @param description the description of the permission
     */
    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * Returns the code of the permission.
     *
     * @return the code
     */
    public String getCode() {
        return code;
    }

    /**
     * Returns the description of the permission.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
}
