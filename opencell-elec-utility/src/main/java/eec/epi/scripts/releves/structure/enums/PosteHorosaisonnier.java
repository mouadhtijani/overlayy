package eec.epi.scripts.releves.structure.enums;

public enum PosteHorosaisonnier {
    BASE("BASE"),
    HP("HP"),
    HC("HC"),
    JOUR("JOUR"),
    NUIT("NUIT ");

    private final String value;

    PosteHorosaisonnier(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
