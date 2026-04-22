package eec.epi.scripts.releves.structure.enums;

public enum NatureReleve {
    DEPOSE("DEPOSE"),
    ESTIME("ESTIME"),
    EVENEMENT("EVENEMENT"),
    POSE("POSE"),
    REEL("REEL");

    private final String value;

    NatureReleve(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}