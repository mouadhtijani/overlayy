package eec.epi.scripts.pds.structure;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pds {
    private String idPds;
    private String numCompteur;
    private String debutValidite;
    private String exploitation;
    private String branchement;
    private String adresse;
    private String tableau;
    private Long roues;
    private Long nbFils;
    private Double coefLecture;
    private Long puissanceTransfo;
    @JsonProperty("IsTransfoAvantBranchement")
    private boolean IsTransfoAvantBranchement;
    private Boolean transfoIntensite;
    private String gamme;
    private Long longueurConducteur;
    private String typeRaccordement;
    private Boolean coefTanPhi;
    private Long primeMensuelRaccordement;
    private Long primeMensuelTransformation;
    private Double classeCompteur;


    public String getIdPds() {
        return idPds;
    }

    public void setIdPds(String idPds) {
        this.idPds = idPds;
    }

    public String getNumCompteur() {
        return numCompteur;
    }

    public void setNumCompteur(String numCompteur) {
        this.numCompteur = numCompteur;
    }


    public String getDebutValidite() {
        return debutValidite;
    }

    public void setDebutValidite(String debutValidite) {
        this.debutValidite = debutValidite;
    }


    public String getExploitation() {
        return exploitation;
    }

    public void setExploitation(String exploitation) {
        this.exploitation = exploitation;
    }

    public String getBranchement() {
        return branchement;
    }

    public void setBranchement(String branchement) {
        this.branchement = branchement;
    }

    public String getAdresse() {
        return adresse;
    }

    public void setAdresse(String adresse) {
        this.adresse = adresse;
    }

    public String getTableau() {
        return tableau;
    }

    public void setTableau(String tableau) {
        this.tableau = tableau;
    }


    public Long getRoues() {
        return roues;
    }

    public void setRoues(Long roues) {
        this.roues = roues;
    }


    public Long getNbFils() {
        return nbFils;
    }

    public void setNbFils(Long nbFils) {
        this.nbFils = nbFils;
    }

    public Double getCoefLecture() {
        return coefLecture;
    }

    public void setCoefLecture(Double coefLecture) {
        this.coefLecture = coefLecture;
    }

    public Long getPuissanceTransfo() {
        return puissanceTransfo;
    }

    public void setPuissanceTransfo(Long puissanceTransfo) {
        this.puissanceTransfo = puissanceTransfo;
    }


    public boolean isIsTransfoAvantBranchement() {
        return IsTransfoAvantBranchement;
    }

    public void setIsTransfoAvantBranchement(boolean IsTransfoAvantBranchement) {
        this.IsTransfoAvantBranchement = IsTransfoAvantBranchement;
    }

    public Boolean isTransfoIntensite() {
        return transfoIntensite;
    }

    public void setTransfodoubleensite(Boolean transfodoubleensite) {
        this.transfoIntensite = transfodoubleensite;
    }

    public String getGamme() {
        return gamme;
    }

    public void setGamme(String gamme) {
        this.gamme = gamme;
    }

    public Long getLongueurConducteur() {
        return longueurConducteur;
    }

    public void setLongueurConducteur(Long longueurConducteur) {
        this.longueurConducteur = longueurConducteur;
    }

    public String getTypeRaccordement() {
        return typeRaccordement;
    }

    public void setTypeRaccordement(String typeRaccordement) {
        this.typeRaccordement = typeRaccordement;
    }

    public Boolean isCoefTanPhi() {
        return coefTanPhi;
    }

    public void setCoefTanPhi(Boolean coefTanPhi) {
        this.coefTanPhi = coefTanPhi;
    }


    public Long getPrimeMensuelRaccordement() {
        return primeMensuelRaccordement;
    }

    public void setPrimeMensuelRaccordement(Long primeMensuelRaccordement) {
        this.primeMensuelRaccordement = primeMensuelRaccordement;
    }

    public Double getClasseCompteur() {
        return classeCompteur;
    }

    public void setClasseCompteur(Double classeCompteur) {
        this.classeCompteur = classeCompteur;
    }

    public Long getPrimeMensuelTransformation() {
        return primeMensuelTransformation;
    }

    public void setPrimeMensuelTransformation(Long primeMensuelTransformation) {
        this.primeMensuelTransformation = primeMensuelTransformation;
    }


}
