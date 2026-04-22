package eec.epi.scripts.releves.structure;

import java.util.ArrayList;
import java.util.List;

public class Releve {
    private Long idreleve;
    private String numCompteur;
    private String numClient;
    private String dateReleve;
    private String natureReleve;
    private String origineReleve;
    private String idPds;
    private String codeReleve;
    private String codeObservation;
    private String codeRfe;
    private String lot;
    private String tournee;
    private String exploitation;
    private ArrayList<DonneesMesure> donneesMesures;

    public Releve() {
    }

    public Releve(String idPds, String natureReleve, String dateReleve, ArrayList<DonneesMesure> donneesMesures) {
        this.idPds = idPds;
        this.natureReleve = natureReleve;
        this.dateReleve = dateReleve;
        this.donneesMesures = donneesMesures;
    }

    public Long getIdReleve() {
        return idreleve;
    }

    public String getNumCompteur() {
        return numCompteur;
    }

    public String getNumClient() {
        return numClient;
    }

    public String getDateReleve() {
        return dateReleve;
    }

    public String getNatureReleve() {
        return natureReleve;
    }

    public String getOrigineReleve() {
        return origineReleve;
    }

    public String getIdPds() {
        return idPds;
    }

    public String getCodeReleve() {
        return codeReleve;
    }

    public String getCodeObservation() {
        return codeObservation;
    }

    public String getCodeRfe() {
        return codeRfe;
    }

    public String getLot() {
        return lot;
    }

    public String getTournee() {
        return tournee;
    }

    public String getExploitation() {
        return exploitation;
    }

    public ArrayList<DonneesMesure> getDonneesMesures() {
        return donneesMesures;
    }

    public void setIdreleve(Long idreleve) {
        this.idreleve = idreleve;
    }

    public void setNumCompteur(String numCompteur) {
        this.numCompteur = numCompteur;
    }

    public void setDateReleve(String dateReleve) {
        this.dateReleve = dateReleve;
    }

    public void setNatureReleve(String natureReleve) {
        this.natureReleve = natureReleve;
    }

    public void setOrigineReleve(String origineReleve) {
        this.origineReleve = origineReleve;
    }

    public void setIdPds(String idPds) {
        this.idPds = idPds;
    }

    public void setCodeReleve(String codeReleve) {
        this.codeReleve = codeReleve;
    }

    public void setCodeObservation(String codeObservation) {
        this.codeObservation = codeObservation;
    }

    public void setCodeRfe(String codeRfe) {
        this.codeRfe = codeRfe;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public void setTournee(String tournee) {
        this.tournee = tournee;
    }

    public void setExploitation(String exploitation) {
        this.exploitation = exploitation;
    }

    public void setDonneesMesures(ArrayList<DonneesMesure> donneesMesures) {
        this.donneesMesures = donneesMesures;
    }

    public void setNumClient(String numClient) {
        this.numClient = numClient;
    }
}
