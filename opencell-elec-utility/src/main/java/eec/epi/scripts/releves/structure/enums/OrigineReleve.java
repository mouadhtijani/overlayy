package eec.epi.scripts.releves.structure.enums;

public enum OrigineReleve {
    MAREL("MAREL"),
    SATURNE("SATURNE"),
    SMARTSIDE("SMARTSIDE"),
    ENERLAKE("ENERLAKE"),
    ADVANCE("ADVANCE"),
    DI("DI"),
    AEL("AEL"),
    TELEPHONE("TELEPHONE"),
    MANUEL("MANUEL");

    private final String value;

    OrigineReleve(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
