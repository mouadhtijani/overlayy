package eec.epi.scripts.releves;

import eec.epi.scripts.ValidatorUtils;
import eec.epi.scripts.releves.structure.DonneesMesure;
import eec.epi.scripts.releves.structure.Releve;
import eec.epi.scripts.releves.structure.enums.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Objects.isNull;

/**
 * This class contains validation methods specific to handling measurement readings and related data.
 * It extends the functionality of the base class ValidatorUtils.
 *
 * @author Ahmed BAHRI ahmed.bahri@iliadeconsulting.com
 */
public class ReleveValidator extends ValidatorUtils {

    // Field constants
    protected static final String ID_PDS = "id_pds";
    protected static final String NUM_COMPTEUR = "num_compteur";
    protected static final String NATURE_MESURE = "nature_mesure";
    protected static final String NATURE_RELEVE = "nature_releve";
    protected static final String DATE_RELEVE = "date_releve";
    protected static final String POSTE_HOROSAISONNIER = "poste_horosaisonnier";

    /**
     * Checks if the provided unit value is a valid member of the Unite enum.
     *
     * @param unite The unit value to be checked.
     * @return True if the unit is not valid, otherwise false.
     */
    public boolean isNotValidUnite(String unite) {
        return !isNullOrEmpty(unite) && isNotValidEnumValue(Unite.class, unite);
    }

    /**
     * Checks if the provided measurement value is valid.
     *
     * @param mesure The measurement value to be checked.
     * @return True if the measurement is not valid, otherwise false.
     */
    public boolean isNotValidMesure(String mesure) {
        return isNullOrEmpty(mesure) || isNullOrEmptyOrNotNumericSTR(mesure);
    }

    /**
     * Checks if the provided nature of measurement value is a valid member of the NatureMesure enum.
     *
     * @param natureMesure The nature of measurement value to be checked.
     * @return True if the nature of measurement is not valid, otherwise false.
     */
    public boolean isNotValidNatureMesure(String natureMesure) {
        return isNullOrEmpty(natureMesure) || isNotValidEnumValue(NatureMesure.class, natureMesure);
    }

    /**
     * Checks if the provided value for the origin of measurement reading is a valid member of the OrigineReleve enum.
     *
     * @param posteHorosaisonnier The origin of measurement reading value to be checked.
     * @return True if the origin of measurement reading is not valid, otherwise false.
     */
    public boolean isNotValidPosteHorosaisonnier(String posteHorosaisonnier) {
        return isNullOrEmpty(posteHorosaisonnier) || isNotValidEnumValue(PosteHorosaisonnier.class, posteHorosaisonnier);
    }

    /**
     * Checks if the provided list of measurement data is valid.
     *
     * @param donneesMesures The list of measurement data to be checked.
     * @return True if the list of measurement data is not valid, otherwise false.
     */
    public boolean isNotValidDonneesMesures(ArrayList<DonneesMesure> donneesMesures) {
        return isNullOrEmpty(donneesMesures);
    }

    /**
     * Checks if the provided tournee value is not valid.
     *
     * @param tournee The tournee value to be checked.
     * @return True if the tournee value is not valid, otherwise false.
     */
    public boolean isNotValidTournee(String tournee) {
        return isNullOrEmptyOrNotNumericSTR(tournee);

    }

    /**
     * Checks if the provided lot value is not valid.
     *
     * @param lot The lot value to be checked.
     * @return True if the lot value is not valid, otherwise false.
     */
    public boolean isNotValidLot(String lot) {
        return isNullOrEmptyOrNotNumericSTR(lot);
    }

    /**
     * Checks if the provided idPds value is not valid.
     *
     * @param idPds The idPds value to be checked.
     * @return True if the idPds value is not valid, otherwise false.
     */
    public boolean isNotValidIdPds(String idPds) {
        return isNullOrEmpty(idPds);
    }

    /**
     * Checks if the provided origineReleve value is not a valid member of the OrigineReleve enum.
     *
     * @param origineReleve The origineReleve value to be checked.
     * @return True if the origineReleve value is not valid, otherwise false.
     */
    public boolean isNotValidOrigineReleve(String origineReleve) {
        return isNullOrEmpty(origineReleve) || isNotValidEnumValue(OrigineReleve.class, origineReleve);
    }

    /**
     * Checks if the provided natureReleve value is not a valid member of the NatureReleve enum.
     *
     * @param natureReleve The natureReleve value to be checked.
     * @return True if the natureReleve value is not valid, otherwise false.
     */
    public boolean isNotValidNatureReleve(String natureReleve) {
        return isNullOrEmpty(natureReleve) || isNotValidEnumValue(NatureReleve.class, natureReleve);
    }

    /**
     * Checks if the provided Releve's natureReleve is not valid for index verification.
     *
     * @param releves The list of relevant map objects.
     * @param releve  The Releve object to be checked.
     * @return True if the Releve's natureReleve is not valid for index verification, otherwise false.
     */
    public boolean isNotValidNatureReleveIndex(List<Map<String, Object>> releves, Releve releve) {
        return isNullOrEmpty(releve.getNatureReleve()) || (!releve.getNatureReleve().equals(NatureReleve.POSE.getValue()) && !verifIndex(releves, releve, false));
    }

    /**
     * Checks if the provided dateReleve value is not valid.
     *
     * @param dateReleve The dateReleve value to be checked.
     * @return True if the dateReleve value is not valid, otherwise false.
     */
    public boolean isNotValidDateReleve(String dateReleve) {
        return isNullOrEmptyOrNotValidDate(dateReleve) || !Pattern.matches("\\d{4}-\\d{2}-\\d{2}", dateReleve);
    }

    /**
     * Checks if the provided numCompteur value is not valid.
     *
     * @param numCompteur The numCompteur value to be checked.
     * @return True if the numCompteur value is not valid, otherwise false.
     */
    public boolean isNotValidNumCompteur(String numCompteur) {
        return isNullOrEmpty(numCompteur);
    }

    /**
     * Checks if the provided idReleve value is null.
     *
     * @param idReleve The idReleve value to be checked.
     * @return True if the idReleve value is null, otherwise false.
     */
    public boolean isNotValidIdReleve(Long idReleve) {
        return isNull(idReleve);
    }
    /**
     * Checks if the provided idReleve value is null.
     *
     * @param num The idReleve value to be checked.
     * @return True if the idReleve value is null, otherwise false.
     */
    public boolean isNotValidNumClient(String num) {
        return isNullOrEmpty(num);
    }

    /**
     * Checks if the provided Releve's dateReleve is not valid for index verification.
     *
     * @param releves The list of relevant map objects.
     * @param releve  The Releve object to be checked.
     * @return True if the Releve's dateReleve is not valid for index verification, otherwise false.
     */
    public boolean isNotValidDateReleveIndex(List<Map<String, Object>> releves, Releve releve) {
        return verifIndex(releves, releve, true);
    }

    /**
     * Private helper method for verifying index constraints for Releve objects.
     *
     * @param releves The list of relevant map objects.
     * @param releve  The Releve object to be checked.
     * @param isSupp  Flag after or before date.
     * @return True if the index constraints are not satisfied, otherwise false.
     */
    public boolean verifIndex(List<Map<String, Object>> releves, Releve releve, boolean isSupp) {
        if (releves == null || releves.isEmpty()) {
            return false;
        }

        String natureReleve = releve.getNatureReleve();
        boolean isNatureMesureValid = isNatureMesureValid(natureReleve);

        List<DonneesMesure> donneesMesures = releve.getDonneesMesures();
        Date releveDate = parseDate(releve.getDateReleve());
        String idPds = releve.getIdPds();
        String numCompteur = releve.getNumCompteur();
        if (isSupp) {
            return releves.stream()
                    .anyMatch(r -> {
                        Date dateReleveTemp = (Date) r.get(DATE_RELEVE);
                        String numCompteurTemp = String.valueOf(r.get(NUM_COMPTEUR));
                        if (isNull(dateReleveTemp)) return false;
                        String idPdsTemp = String.valueOf(r.get(ID_PDS));
                        boolean dateReleveSupp = releveDate.before(dateReleveTemp);
                        boolean isExistant = isExistantDonneesMesures(r, donneesMesures);
                        return idPdsTemp != null && numCompteurTemp != null && idPdsTemp.equals(idPds) && numCompteurTemp.equals(numCompteur) && isNatureMesureValid && dateReleveSupp && isExistant;
                    });
        } else {
            return releves.stream()
                    .anyMatch(r -> {
                        Date dateReleveTemp = (Date) r.get(DATE_RELEVE);
                        String numCompteurTemp = String.valueOf(r.get(NUM_COMPTEUR));
                        if (isNull(dateReleveTemp)) return false;
                        String idPdsTemp = String.valueOf(r.get(ID_PDS));
                        return idPdsTemp != null && numCompteurTemp != null && idPdsTemp.equals(idPds) && numCompteurTemp.equals(numCompteur) && NatureReleve.POSE.getValue().equals(r.get("nature_releve").toString()) && releveDate.after(dateReleveTemp);
                    });
        }
    }

    private boolean isNatureMesureValid(String natureReleve) {
        return (
                NatureReleve.REEL.getValue().equals(natureReleve) || NatureReleve.DEPOSE.getValue().equals(natureReleve) || NatureReleve.POSE.getValue().equals(natureReleve))
                        ;
    }

    private boolean isExistantDonneesMesures(Map<String, Object> r, List<DonneesMesure> donneesMesures) {
        return donneesMesures.stream()
                .anyMatch(donneesMesure -> {
                    Object natureMesure = r.get(NATURE_MESURE);
                    Object posteHorosaisonniernier = r.get(POSTE_HOROSAISONNIER);
                    return natureMesure != null && posteHorosaisonniernier != null &&
                            (natureMesure.equals(donneesMesure.getNatureMesure()) ||
                                    posteHorosaisonniernier.equals(donneesMesure.getPosteHorosaisonnier()));
                });
    }



}
