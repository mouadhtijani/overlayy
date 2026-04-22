package eec.epi.scripts.releves.structure.enums;

public enum Unite {
    kVA("kVA"),
    kW("kW"),
    h("h"),
    min("min"),
    m3("m3"),
    L("L"),
    A("A");

    private final String value;

    Unite(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
