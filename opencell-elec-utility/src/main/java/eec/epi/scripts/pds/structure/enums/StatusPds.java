package eec.epi.scripts.pds.structure.enums;

public enum StatusPds {
    ACTIF("ACTIF"),
    INACTIF("INACTIF");

    private final String value;

    StatusPds(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}