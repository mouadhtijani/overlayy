package eec.epi.scripts.conso;


import eec.epi.scripts.JobScript;
import org.meveo.admin.exception.BusinessException;
import org.meveo.service.custom.CustomTableService;

import java.util.List;
/**
 * @author Sarah khabthani sarah.khabthani@iliadeconsulting.com
 */
public class Calcul_conso extends JobScript {
    private final CustomTableService customTableService = getServiceInterface(CustomTableService.class);
    protected static final String INSERT_INDEX_QUANTITY_POSITIF_QUERY = "INSERT INTO public.ct_conso(id_releve_temp,exploitation, num_contrat,lot,tournee, nom_client, quantite, id_pds, num_compteur, index_precedent, index_nouveau, date_index_precedent, date_releve, nature_releve, origine, poste_horosaisonnier, nature_mesure, unite, statut)\n" +
            "SELECT " +
            "t1.id,\n"+
            "t1.exploitation,\n" +
            "       sub.code as num_contrat,\n" +
            "                   t1.lot,\n" +
            "                   t1.tournee,\n" +
            "                    cus.firstname || ' ' || cus.lastname AS nom_client,\n" +
            "                   CASE\n" +
            "                       WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA', 'ER') AND\n" +
            "                            (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric >= 0) THEN (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                       WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA', 'ER') AND\n" +
            "                            (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric >= 0) THEN (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                       WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND t1.nature_mesure IN ('EA', 'ER') AND\n" +
            "                            (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric >= 0) THEN (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                       WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND\n" +
            "                            t1.nature_mesure IN ('EA', 'ER') AND (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric >= 0)\n" +
            "                           THEN (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                       END AS quantite,\n" +
            "                   t1.id_pds,\n" +
            "                   t1.num_compteur,\n" +
            "                   CASE WHEN t2.index_surcharge IS NOT NULL THEN t2.index_surcharge ELSE t2.mesure END AS index_precedent,\n" +
            "                   CASE WHEN t1.index_surcharge IS NOT NULL THEN t1.index_surcharge ELSE t1.mesure END AS index_nouveau,\n" +
            "                   t2.date_releve AS date_index_precedent,\n" +
            "                   t1.date_releve,\n" +
            "                   t1.nature_releve,\n" +
            "                   t1.origine,\n" +
            "                   t1.poste_horosaisonnier,\n" +
            "                   t1.nature_mesure,\n" +
            "                   CASE\n" +
            "                       WHEN t1.nature_mesure IN ('EA', 'EAi') THEN 'kWh'\n" +
            "                       WHEN t1.nature_mesure IN ('ER', 'ERi') THEN 'kVArh'\n" +
            "                       ELSE NULL\n" +
            "                       END   AS unite,\n" +
            "                   'IMPORTE' AS statut\n" +
            "            FROM public.ct_releves t1\n" +
            "                     JOIN (SELECT id_pds,\n" +
            "                                  nature_mesure,\n" +
            "                                  poste_horosaisonnier,\n" +
            "                                  num_compteur,\n" +
            "                                  MAX(date_releve) AS max_date_releve\n" +
            "                           FROM public.ct_releves\n" +
            "                           GROUP BY id_pds, nature_mesure, num_compteur,poste_horosaisonnier) t2_max ON t1.id_pds = t2_max.id_pds\n" +
            "                      JOIN medina_access ma ON t1.id_pds = ma.acces_user_id\n" +
            "                      JOIN billing_subscription sub ON ma.subscription_id = sub.id\n" +
            "                      JOIN billing_user_account UA ON sub.user_account_id = UA.id\n" +
            "                      JOIN billing_billing_account BA ON UA.billing_account_id = BA.id\n" +
            "                      JOIN ar_customer_account CA ON BA.customer_account_id = CA.id\n" +
            "                      JOIN crm_customer cus ON CA.customer_id = cus.id\n" +
            "                AND t1.nature_mesure = t2_max.nature_mesure\n" +
            "                AND t1.poste_horosaisonnier = t2_max.poste_horosaisonnier\n" +
            "                AND t1.num_compteur = t2_max.num_compteur\n" +
            "                     JOIN public.ct_releves t2 ON t2.id_pds = t1.id_pds\n" +
            "                AND t2.nature_mesure = t1.nature_mesure\n" +
            "                AND t2.poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "                AND t2.num_compteur = t1.num_compteur\n" +
            "                AND t2.date_releve = (\n" +
            "                    SELECT MAX(date_releve)\n" +
            "                    FROM public.ct_releves\n" +
            "                    WHERE id_pds = t1.id_pds\n" +
            "                      AND nature_mesure = t1.nature_mesure\n" +
            "                      AND poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "                      AND num_compteur = t1.num_compteur\n" +
            "                      AND date_releve < t1.date_releve\n" +
            "                )\n" +
            "            WHERE t1.nature_mesure IN ('EA', 'ER')\n" +
            "              AND t1.nature_releve IN ('REEL', 'EVENEMENT', 'DEPOSE')\n" +
            "              AND t1.statut_releve = 'VALIDE'\n" +
            "              AND t1.id_pds IS NOT NULL\n" +
            "              AND (t1.mesure IS NOT NULL AND t2.mesure IS NOT NULL)\n" +
            "              AND (\n" +
            "                        (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND\n" +
            "                        ((t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) >= 0)\n" +
            "                    OR (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND\n" +
            "                       ((t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) >= 0)\n" +
            "                    OR (t2.index_surcharge IS NULL AND t1.index_surcharge IS NOT NULL) AND\n" +
            "                       ((t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) >= 0)\n" +
            "                    OR (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND\n" +
            "                       ((t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric >= 0))\n" +
            "                );";
    protected static final String INSERT_INDEX_QUANTITY_NEG_CADRAN_TOUR_QUERY = "INSERT INTO public.ct_conso(id_releve_temp,exploitation, num_contrat,lot,tournee, nom_client, quantite, id_pds, num_compteur, index_precedent, index_nouveau, date_index_precedent, date_releve, nature_releve, origine, poste_horosaisonnier, nature_mesure, unite, statut)\n" +
            "            WITH MaxReleves AS (\n" +
            "                SELECT\n" +
            "                    id_pds,\n" +
            "                    nature_mesure,\n" +
            "                    poste_horosaisonnier,\n" +
            "                    num_compteur,\n" +
            "                    MAX(date_releve) AS max_date_releve\n" +
            "                FROM public.ct_releves\n" +
            "                GROUP BY id_pds, nature_mesure, num_compteur, poste_horosaisonnier\n" +
            "            )\n" +
            "            \n" +
            "            SELECT\n" +
            "             t1.id,\n"+
            "                t1.exploitation,\n" +
            "                sub.code as num_contrat,\n" +
            "                t1.lot,\n" +
            "                t1.tournee,\n" +
            "                cus.firstname || ' ' || cus.lastname AS nom_client,\n" +
            "                CASE\n" +
            "                    WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric < t2.mesure\\:\\:numeric\n" +
            "                        THEN ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric + t1.mesure\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                    WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds) - t2.mesure\\:\\:numeric < t2.mesure\\:\\:numeric\n" +
            "                        THEN ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric + t1.index_surcharge\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                    WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric < t2.index_surcharge\\:\\:numeric\n" +
            "                        THEN ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric + t1.mesure\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                    WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric < t2.index_surcharge\\:\\:numeric\n" +
            "                        THEN ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric + t1.index_surcharge\\:\\:numeric) * (SELECT pds.coefficient_lecture\\:\\:numeric FROM public.ct_pds pds WHERE (pds.id_pds = t1.id_pds AND pds.num_compteur=t1.num_compteur and pds.statut_pds='ACTIF'))\n" +
            "                    END AS quantite,\n" +
            "                t1.id_pds,\n" +
            "                t1.num_compteur,\n" +
            "                CASE\n" +
            "                    WHEN t2.index_surcharge IS NOT NULL THEN t2.index_surcharge\n" +
            "                    WHEN t2.index_surcharge IS NULL THEN t2.mesure\n" +
            "                    END AS index_precedent,\n" +
            "                CASE\n" +
            "                    WHEN t1.index_surcharge IS NOT NULL THEN t1.index_surcharge\n" +
            "                    WHEN t1.index_surcharge IS NULL THEN t1.mesure\n" +
            "                    END AS index_nouveau,\n" +
            "                t2.date_releve AS date_index_precedent,\n" +
            "                t1.date_releve,\n" +
            "                t1.nature_releve,\n" +
            "                t1.origine,\n" +
            "                t1.poste_horosaisonnier,\n" +
            "                t1.nature_mesure,\n" +
            "                CASE\n" +
            "                    WHEN t1.nature_mesure IN ('EA', 'EAi') THEN 'kWh'\n" +
            "                    WHEN t1.nature_mesure IN ('ER', 'ERi') THEN 'kVArh'\n" +
            "                    END AS unite,\n" +
            "                'IMPORTE' AS statut\n" +
            "            FROM\n" +
            "                public.ct_releves t1\n" +
            "                    JOIN MaxReleves t2_max ON t1.id_pds = t2_max.id_pds\n" +
            "                    AND t1.nature_mesure = t2_max.nature_mesure\n" +
            "                    AND t1.poste_horosaisonnier = t2_max.poste_horosaisonnier\n" +
            "                    AND t1.num_compteur = t2_max.num_compteur\n" +
            "                    JOIN public.ct_releves t2 ON t2.id_pds = t1.id_pds\n" +
            "                    AND t2.nature_mesure = t1.nature_mesure\n" +
            "                    AND t2.poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "                    AND t2.num_compteur = t1.num_compteur\n" +
            "                    AND t2.date_releve = (\n" +
            "                        SELECT MAX(date_releve)\n" +
            "                        FROM public.ct_releves\n" +
            "                        WHERE id_pds = t1.id_pds\n" +
            "                          AND nature_mesure = t1.nature_mesure\n" +
            "                          AND poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "                          AND num_compteur = t1.num_compteur\n" +
            "                          AND date_releve < t1.date_releve\n" +
            "                    )\n" +
            "                    JOIN medina_access ma ON t1.id_pds = ma.acces_user_id\n" +
            "                    JOIN billing_subscription sub ON ma.subscription_id = sub.id\n" +
            "                    JOIN billing_user_account UA ON sub.user_account_id = UA.id\n" +
            "                    JOIN billing_billing_account BA ON UA.billing_account_id = BA.id\n" +
            "                    JOIN ar_customer_account CA ON BA.customer_account_id = CA.id\n" +
            "                    JOIN crm_customer cus ON CA.customer_id = cus.id\n" +
            "            WHERE\n" +
            "                    t1.nature_mesure IN ('EA', 'ER')\n" +
            "              AND t1.nature_releve IN ('REEL', 'EVENEMENT', 'DEPOSE')\n" +
            "              AND t1.statut_releve = 'VALIDE'\n" +
            "              AND t1.id_pds IS NOT NULL\n" +
            "              AND (t1.mesure IS NOT NULL AND t2.mesure IS NOT NULL)\n" +
            "              AND (\n" +
            "                        (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric < t2.mesure\\:\\:numeric)\n" +
            "                    OR (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric < t2.index_surcharge\\:\\:numeric)\n" +
            "                    OR (t2.index_surcharge IS NULL AND t1.index_surcharge IS NOT NULL) AND (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric < t2.mesure\\:\\:numeric)\n" +
            "                    OR (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric < 0) AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric < t2.index_surcharge\\:\\:numeric)\n" +
            "                );";
    protected static final String INSERT_INDEX_QUANTITY_NEG_INCORRECT_ENTRY_QUERY = "SELECT\n" +
            "    CASE\n" +
            "        WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric >= t2.mesure\\:\\:numeric\n" +
            "            THEN NULL\n" +
            "        WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric >= t2.mesure\\:\\:numeric\n" +
            "            THEN NULL\n" +
            "        WHEN (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric >= t2.index_surcharge\\:\\:numeric\n" +
            "            THEN NULL\n" +
            "        WHEN (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND t1.nature_mesure IN ('EA','ER') AND (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND (SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric >= t2.index_surcharge\\:\\:numeric\n" +
            "            THEN NULL\n" +
            "        END AS quantite,\n" +
            "    t1.id_releve as id_releve,\n" +
            "    t1.id_pds as id_pds,\n" +
            "    CASE\n" +
            "        WHEN t2.index_surcharge IS NOT NULL THEN t2.index_surcharge\n" +
            "        WHEN t2.index_surcharge IS NULL THEN t2.mesure\n" +
            "        END AS index_precedent,\n" +
            "    CASE\n" +
            "        WHEN t1.index_surcharge IS NOT NULL THEN t1.index_surcharge\n" +
            "        WHEN t1.index_surcharge IS NULL THEN t1.mesure\n" +
            "        END AS index_nouveau,\n" +
            "    t2.date_releve as date_index_precedent,\n" +
            "    t1.date_releve as date_releve,\n" +
            "    t1.poste_horosaisonnier as poste_horosaisonnier,\n" +
            "    t1.nature_mesure as nature_mesure\n" +
            "FROM public.ct_releves t1\n" +
            "         JOIN (\n" +
            "    SELECT\n" +
            "        id_pds,\n" +
            "        nature_mesure,\n" +
            "        poste_horosaisonnier,\n" +
            "        num_compteur,\n" +
            "        MAX(date_releve) as max_date_releve\n" +
            "    FROM public.ct_releves\n" +
            "--     WHERE statut_releve = 'CALCULE'\n" +
            "    GROUP BY id_pds, nature_mesure, num_compteur,poste_horosaisonnier\n" +
            ") t2_max ON t1.id_pds = t2_max.id_pds\n" +
            "    AND t1.nature_mesure = t2_max.nature_mesure\n" +
            "    AND t1.poste_horosaisonnier = t2_max.poste_horosaisonnier\n" +
            "    AND t1.num_compteur = t2_max.num_compteur\n" +
            "         JOIN public.ct_releves t2 ON t2.id_pds = t1.id_pds\n" +
            "    AND t2.nature_mesure = t1.nature_mesure\n" +
            "    AND t2.poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "    AND t2.num_compteur = t1.num_compteur\n" +
            "    AND t2.date_releve = (\n" +
            "        SELECT MAX(date_releve)\n" +
            "        FROM public.ct_releves\n" +
            "        WHERE id_pds = t1.id_pds\n" +
            "          AND nature_mesure = t1.nature_mesure\n" +
            "          AND poste_horosaisonnier = t1.poste_horosaisonnier\n" +
            "          AND num_compteur = t1.num_compteur\n" +
            "          AND date_releve < t1.date_releve\n" +
            "    )\n" +
            "WHERE\n" +
            "        t1.nature_mesure IN ('EA', 'ER')\n" +
            "  AND t1.nature_releve IN ('REEL', 'EVENEMENT', 'DEPOSE')\n" +
            "  AND t1.statut_releve = 'VALIDE'\n" +
            "  AND t1.id_pds IS NOT NULL\n" +
            "  AND (t1.mesure IS NOT NULL AND t2.mesure IS NOT NULL)\n" +
            "  AND t1.id_releve IS NOT NULL\n" +
            "  AND (\n" +
            "            (t1.index_surcharge IS NULL AND t2.index_surcharge IS NULL) AND (t1.mesure\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric >= t2.mesure\\:\\:numeric)\n" +
            "        OR (t1.index_surcharge IS NULL AND t2.index_surcharge IS NOT NULL) AND (t1.mesure\\:\\:numeric - t2.index_surcharge\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric >= t2.index_surcharge\\:\\:numeric)\n" +
            "        OR (t2.index_surcharge IS NULL AND t1.index_surcharge IS NOT NULL) AND (t1.index_surcharge\\:\\:numeric - t2.mesure\\:\\:numeric) < 0 AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.mesure\\:\\:numeric >= t2.mesure\\:\\:numeric)\n" +
            "        OR (t1.index_surcharge IS NOT NULL AND t2.index_surcharge IS NOT NULL) AND (t1.index_surcharge\\:\\:numeric - t2.index_surcharge\\:\\:numeric < 0) AND ((SELECT POWER(10, pds.roues) FROM public.ct_pds pds WHERE pds.id_pds = t1.id_pds and pds.statut_pds='ACTIF') - t2.index_surcharge\\:\\:numeric >= t2.index_surcharge\\:\\:numeric)\n" +
            "    );\n";

    protected static final String INSERT_MESURE_QUERY = "INSERT INTO public.ct_conso(id_releve_temp,exploitation,num_contrat,lot,tournee,nom_client,quantite, id_pds,num_compteur, date_releve, nature_releve, origine, poste_horosaisonnier,\n" +
            "                        nature_mesure, unite, statut)\n" +
            "                        SELECT\n" +
            "                          t1.id,\n"+
            "                        t1.exploitation as exploitation,\n" +
            "                        sub.code as num_contrat,\n" +
            "                t1.lot as lot,\n" +
            "                t1.tournee as tournee,\n" +
            "                cus.firstname || ' ' || cus.lastname AS nom_client,\n" +
            "                           t1.mesure\\:\\:numeric as quantite,\n" +
            "                           t1.id_pds as id_pds,\n" +
            "                           t1.num_compteur,\n"+
            "                           t1.date_releve as date_releve,\n" +
            "                           t1.nature_releve as nature_releve,\n" +
            "                           t1.origine as origine,\n" +
            "                           t1.poste_horosaisonnier as poste_horosaisonnier,\n" +
            "                           t1.nature_mesure as nature_mesure,\n" +
            "                           t1.unite as unite,\n" +
            "                           'IMPORTE' as statut\n" +
            "                        FROM public.ct_releves t1\n" +
            "                                 JOIN medina_access ma ON t1.id_pds = ma.acces_user_id\n" +
            "                                 JOIN billing_subscription sub ON ma.subscription_id = sub.id\n" +
            "                                 JOIN billing_user_account UA ON sub.user_account_id = UA.id\n" +
            "                                 JOIN billing_billing_account BA ON UA.billing_account_id = BA.id\n" +
            "                                 JOIN ar_customer_account CA ON BA.customer_account_id = CA.id\n" +
            "                                 JOIN crm_customer cus ON CA.customer_id = cus.id\n" +
            "                        WHERE t1.statut_releve = 'VALIDE' AND t1.nature_mesure NOT IN ('EA','ER') AND t1.id_pds IS NOT NULL\n" +
            "                         AND t1.nature_releve IN ('REEL','EVENEMENT','DEPOSE');";
    protected static final String UPDATE_QUERY = "update public.ct_releves set statut_releve='CALCULE' where id_pds in (select id_pds from public.ct_conso where statut='IMPORTE') AND ((message_erreur IS NULL OR message_erreur='') OR (code_erreur IS NULL OR code_erreur='') ) and id in (select id_releve_temp from public.ct_conso);\n";
    protected static final String UPDATE_ERROR = "update public.ct_releves set statut_releve='ERREUR', code_erreur='INDEX001', message_erreur='Erreur de saisie index'\n" +
            "WHERE id_releve= :id AND statut_releve='VALIDE' AND id not in (select id_releve_temp from public.ct_conso)";

    @Override
    public void execute(JobContext jobContext) {
        try {

            updateOK(jobContext, INSERT_INDEX_QUANTITY_POSITIF_QUERY, "index calculé avec succès");

            updateOK(jobContext, INSERT_MESURE_QUERY, "mesure calculée avec succès");
            updateOK(jobContext, INSERT_INDEX_QUANTITY_NEG_CADRAN_TOUR_QUERY, "index tour de cadran calculé avec succès");
            List<Object[]> findIncorrectEntry = em().createNativeQuery(INSERT_INDEX_QUANTITY_NEG_INCORRECT_ENTRY_QUERY).getResultList();
            if (!findIncorrectEntry.isEmpty()) {
                for (Object[] entry : findIncorrectEntry) {
                    Object id = entry[1];
                    jobContext.reportKO("Erreur de saisie index " + entry[1]);
                    em().createNativeQuery(UPDATE_ERROR)
                            .setParameter("id", id)
                            .executeUpdate();
                }
            }
            customTableService.getEntityManager().createNativeQuery(UPDATE_QUERY).executeUpdate();

        }catch (Exception e){
            throw new BusinessException(e);
        }


    }

}
