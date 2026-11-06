package es.uclm.esi.iso2.common.access.domain;

public class Permission {
    private final String code;
    private final String description;

    public Permission(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
