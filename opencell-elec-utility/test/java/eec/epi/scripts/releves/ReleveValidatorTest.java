//package eec.epi.scripts.releves;
//
//import eec.epi.scripts.DataParser;
//import eec.epi.scripts.releves.structure.DonneesMesure;
//import eec.epi.scripts.releves.structure.Releve;
//import org.junit.Test;
//import org.meveo.admin.exception.BusinessException;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import static org.junit.Assert.*;
//
//public class ReleveValidatorTest extends DataParser {
//
//    private final ReleveValidator releveValidator = new ReleveValidator();
//
//    @Test
//    public void testIsNotValidUnite() {
//        assertFalse(releveValidator.isNotValidUnite(""));
//        assertFalse(releveValidator.isNotValidUnite("kVA"));
//        assertFalse(releveValidator.isNotValidUnite("kW"));
//        assertFalse(releveValidator.isNotValidUnite("h"));
//        assertFalse(releveValidator.isNotValidUnite("min"));
//        assertFalse(releveValidator.isNotValidUnite("m3"));
//        assertFalse(releveValidator.isNotValidUnite("L"));
//        assertFalse(releveValidator.isNotValidUnite("A"));
//        assertTrue(releveValidator.isNotValidUnite("INVALID"));
//    }
//
//    @Test
//    public void testIsNotValidMesure() {
//        assertTrue(releveValidator.isNotValidMesure(null));
//        assertTrue(releveValidator.isNotValidMesure(""));
//        assertTrue(releveValidator.isNotValidMesure("Valid Mesure"));
//        assertFalse(releveValidator.isNotValidMesure("159753"));
//    }
//
//    @Test
//    public void testIsNotValidNatureMesure() {
//        assertFalse(releveValidator.isNotValidNatureMesure("EA"));
//        assertFalse(releveValidator.isNotValidNatureMesure("ER"));
//        assertFalse(releveValidator.isNotValidNatureMesure("EAi"));
//        assertFalse(releveValidator.isNotValidNatureMesure("ERi"));
//        assertFalse(releveValidator.isNotValidNatureMesure("Imax"));
//        assertFalse(releveValidator.isNotValidNatureMesure("PA"));
//        assertFalse(releveValidator.isNotValidNatureMesure("DD"));
//        assertFalse(releveValidator.isNotValidNatureMesure("VOL"));
//        assertFalse(releveValidator.isNotValidNatureMesure("VOLR"));
//        assertTrue(releveValidator.isNotValidNatureMesure(null));
//        assertTrue(releveValidator.isNotValidNatureMesure("INVALID"));
//    }
//
//    @Test
//    public void testIsNotValidPosteHorosaisonnier() {
//        assertFalse(releveValidator.isNotValidPosteHorosaisonnier("BASE"));
//        assertFalse(releveValidator.isNotValidPosteHorosaisonnier("HP"));
//        assertFalse(releveValidator.isNotValidPosteHorosaisonnier("HC"));
//        assertFalse(releveValidator.isNotValidPosteHorosaisonnier("JOUR"));
//        assertFalse(releveValidator.isNotValidPosteHorosaisonnier("NUIT"));
//        assertTrue(releveValidator.isNotValidPosteHorosaisonnier(null));
//        assertTrue(releveValidator.isNotValidPosteHorosaisonnier("INVALID"));
//    }
//
//    @Test
//    public void testIsNotValidDonneesMesures() {
//        assertTrue(releveValidator.isNotValidDonneesMesures(null));
//        assertTrue(releveValidator.isNotValidDonneesMesures(new ArrayList<>()));
//        ArrayList<DonneesMesure> donneesMesures = new ArrayList<>();
//        donneesMesures.add(new DonneesMesure());
//        assertFalse(releveValidator.isNotValidDonneesMesures(donneesMesures));
//    }
//
//    @Test
//    public void testIsNotValidTournee() {
//        assertTrue(releveValidator.isNotValidTournee(null));
//        assertTrue(releveValidator.isNotValidTournee(""));
//        assertTrue(releveValidator.isNotValidTournee("abc"));
//        assertFalse(releveValidator.isNotValidTournee("123"));
//    }
//
//    @Test
//    public void testIsNotValidLot() {
//        assertTrue(releveValidator.isNotValidLot(null));
//        assertTrue(releveValidator.isNotValidLot(""));
//        assertTrue(releveValidator.isNotValidLot("abc"));
//        assertFalse(releveValidator.isNotValidLot("123"));
//    }
//
//    @Test
//    public void testIsNotValidIdPds() {
//        assertTrue(releveValidator.isNotValidIdPds(null));
//        assertTrue(releveValidator.isNotValidIdPds(""));
//        assertFalse(releveValidator.isNotValidIdPds("ID123"));
//    }
//
//    @Test
//    public void testIsNotValidOrigineReleve() {
//        assertFalse(releveValidator.isNotValidOrigineReleve("MAREL"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("SATURNE"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("SMARTSIDE"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("ENERLAKE"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("ADVANCE"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("DI"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("AEL"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("TELEPHONE"));
//        assertFalse(releveValidator.isNotValidOrigineReleve("MANUEL"));
//        assertTrue(releveValidator.isNotValidOrigineReleve(null));
//        assertTrue(releveValidator.isNotValidOrigineReleve("INVALID"));
//
//    }
//
//    @Test
//    public void testIsNotValidNatureReleve() {
//        assertFalse(releveValidator.isNotValidNatureReleve("DEPOSE"));
//        assertFalse(releveValidator.isNotValidNatureReleve("ESTIME"));
//        assertFalse(releveValidator.isNotValidNatureReleve("EVENEMENT"));
//        assertFalse(releveValidator.isNotValidNatureReleve("POSE"));
//        assertFalse(releveValidator.isNotValidNatureReleve("REEL"));
//        assertTrue(releveValidator.isNotValidNatureReleve(null));
//        assertTrue(releveValidator.isNotValidNatureReleve("INVALID"));
//    }
//
//    @Test
//    public void testIsNotValidDateReleve() {
//        assertTrue(releveValidator.isNotValidDateReleve(null));
//        assertTrue(releveValidator.isNotValidDateReleve(""));
//        assertTrue(releveValidator.isNotValidDateReleve("2023-08-16d"));
//        assertTrue(releveValidator.isNotValidDateReleve("2023-08/16"));
//        assertTrue(releveValidator.isNotValidDateReleve("2023-08-16"));
//        assertFalse(releveValidator.isNotValidDateReleve("2023/08/16"));
//    }
//
//    @Test
//    public void testIsNotValidNumCompteur() {
//        assertTrue(releveValidator.isNotValidNumCompteur(null));
//        assertTrue(releveValidator.isNotValidNumCompteur(""));
//        assertFalse(releveValidator.isNotValidNumCompteur("12345"));
//    }
//
//    @Test
//    public void testIsNotValidIdReleve() {
//        assertTrue(releveValidator.isNotValidIdReleve(null));
//        assertFalse(releveValidator.isNotValidIdReleve(123L));
//    }
//
//    @Test
//    public void testVerifIndex_ValidReleve() { //TODO
//        List<Map<String, Object>> releves = new ArrayList<>();
//        Map<String, Object> releveData = Map.of(
//                "id_pds", "123",
//                "nature_releve", "REEL",
//                "poste_horosaisonnier", "BASE",
//                "nature_mesure", "EA",
//                "date_releve", parseDate("2023/06/05")
//        );
//        releves.add(releveData);
//        ArrayList<DonneesMesure> donneesMesures = new ArrayList<>();
//        donneesMesures.add(new DonneesMesure("BASE", "EA"));
//        //id_pds, nature_mesure, poste_horosaisonnier => Same
//        //nature_releve = [REEL, POSE, DEPOSE]
//        //date new releve > date old relev
//        Releve releveREEL = new Releve("123","REEL","2023/06/06",donneesMesures);
//        assertFalse(releveValidator.isNotValidDateReleveIndex(releves, releveREEL));
//        //id_pds, nature_mesure, poste_horosaisonnier => Same
//        //nature_releve = [REEL, POSE, DEPOSE]
//        //date new releve < date old relev
//        releveREEL = new Releve("123","REEL","2023/06/04",donneesMesures);
//        assertTrue(releveValidator.isNotValidDateReleveIndex(releves, releveREEL));
//        //id_pds is not the same
//        // nature_mesure, poste_horosaisonnier => Same
//        //nature_releve = [REEL, POSE, DEPOSE]
//        Releve relevePOSE = new Releve("1234","POSE","2023/06/07",donneesMesures);
//        assertFalse(releveValidator.isNotValidDateReleveIndex(releves, relevePOSE));
//        //id_pds is not the same
//        //nature_mesure, poste_horosaisonnier => Same
//        //nature_releve is not in  [REEL, POSE, DEPOSE]
//        Releve releveESTIME  = new Releve("123","ESTIME","2023/06/04",donneesMesures);
//        assertFalse(releveValidator.isNotValidDateReleveIndex(releves, releveESTIME));
//        //id_pds is not the same   isNotValidNatureReleveIndex
//        //nature_mesure, poste_horosaisonnier => Same
//        //nature_releve is not in  [REEL, POSE, DEPOSE]
//        Releve releveWithoutDate  = new Releve("123","ESTIME","2023/06/08",donneesMesures);
//        assertFalse(releveValidator.isNotValidDateReleveIndex(releves, releveWithoutDate));
//    }
//    @Test
//    public void testIsNotValidNatureReleveIndex() { //TODO
//        List<Map<String, Object>> releves = new ArrayList<>();
//        Map<String, Object> releveData = Map.of(
//                "id_pds", "123",
//                "nature_releve", "REEL",
//                "poste_horosaisonnier", "BASE",
//                "nature_mesure", "EA",
//                "date_releve", parseDate("2023/06/05")
//        );
//        releves.add(releveData);
//        ArrayList<DonneesMesure> donneesMesures = new ArrayList<>();
//        donneesMesures.add(new DonneesMesure("BASE", "EA"));
//
//        //natureReleve ≠ POSE
//        //id_pds, nature_mesure, poste_horosaisonnier => Same
//        //date new releve > date old relev
//        Releve releveBefor = new Releve("123","REEL","2023/06/08",donneesMesures);
//        assertTrue(releveValidator.isNotValidNatureReleveIndex(releves, releveBefor));
//        //id_pds is not the same
//        //nature_mesure, poste_horosaisonnier => Same
//        //natureReleve ≠ POSE
//        Releve newIDreleve = new Releve("ID1234","REEL","2023/06/04",donneesMesures);
//        assertTrue(releveValidator.isNotValidNatureReleveIndex(releves, newIDreleve));
//
//        //nature_releve = POSE
//        //id_pds, nature_mesure, poste_horosaisonnier => Same
//        Releve relevePOSE = new Releve("123","POSE","2023/06/08",donneesMesures);
//        assertFalse(releveValidator.isNotValidNatureReleveIndex(releves, relevePOSE));
//
//
//        //nature_mesure, poste_horosaisonnier,id_pds => Same
//        //nature_releve is not in  [REEL, POSE, DEPOSE]
//        Releve releveESTIME  = new Releve("123","ESTIME","2023/06/04",donneesMesures);
//        assertTrue(releveValidator.isNotValidNatureReleveIndex(releves, releveESTIME));
//
//        //natureReleve ≠ POSE
//        //id_pds, nature_mesure, poste_horosaisonnier => Same
//        //date new releve > date old relev
//        Releve releveAfter = new Releve("123","REEL","2023/06/07",donneesMesures);
//        assertTrue(releveValidator.isNotValidNatureReleveIndex(releves, releveAfter));
//
//    }
//}