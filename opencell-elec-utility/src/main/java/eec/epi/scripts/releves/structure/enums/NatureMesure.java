package eec.epi.scripts.releves.structure.enums;

public enum NatureMesure {
    EA("EA"),
    ER("ER"),
    EAi("EAi"),
    ERi("ERi"),
    Imax("Imax"),
    PA("PA"),
    DD("DD"),
    VOL("VOL"),
    VOLR("VOLR ");

    private final String value;

    NatureMesure(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
