package eec.epi.scripts.releves.structure.enums;

public enum Status {
    SUCCESS("TRAITE"),
    FAIL("ERREUR"),
    A_TRAITER("A_TRAITER"),
    VALIDE("VALIDE"),
    CALCULE("CALCULE");

    private final String value;

    Status(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}