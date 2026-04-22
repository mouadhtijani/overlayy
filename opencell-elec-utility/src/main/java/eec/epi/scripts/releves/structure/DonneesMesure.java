package eec.epi.scripts.releves.structure;

public class DonneesMesure {
    private String posteHorosaisonnier;
    private String natureMesure;

    private String mesure;
    private String unite;

    public DonneesMesure() {
    }

    public DonneesMesure(String posteHorosaisonnier, String natureMesure) {
        this.posteHorosaisonnier = posteHorosaisonnier;
        this.natureMesure = natureMesure;
    }

    public String getPosteHorosaisonnier() {
        return posteHorosaisonnier;
    }

    public String getNatureMesure() {
        return natureMesure;
    }

    public String getMesure() {
        return mesure;
    }

    public String getUnite() {
        return unite;
    }
    public void setPosteHorosaisonnier(String posteHorosaisonnier) {
        this.posteHorosaisonnier = posteHorosaisonnier;
    }

    public void setNatureMesure(String natureMesure) {
        this.natureMesure = natureMesure;
    }

    public void setMesure(String mesure) {
        this.mesure = mesure;
    }

    public void setUnite(String unite) {
        this.unite = unite;
    }

}