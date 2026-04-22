
-- Part Energie
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (1, 'PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'BVE_BT', 'BT', NULL, '2024-01-01', '9999-12-31', 8.24),
    (2, 'PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'BVE_BT', 'BT', NULL, '2024-01-01', '9999-12-31', 20.60),
    (3, 'PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'BVE_HTA', 'HTA', NULL, '2024-01-01', '9999-12-31', 8.24),
    (4, 'PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'BVE_HTA', 'HTA', NULL, '2024-01-01', '9999-12-31', 20.60),
    (5, 'PX_EA_HP', 'Prix energie heures pleines', 'IRRIGATION', 'BT', NULL, '2024-01-01', '9999-12-31', 30.74),
    (6, 'PX_EA_HC', 'Prix energie heures creuses', 'IRRIGATION', 'BT', NULL, '2024-01-01', '9999-12-31', 10.25);

-- Prime fixe
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (7, 'PX_PRIMEFIXE_BT', 'Prix prime fixe', 'BVE_BT', 'BT', NULL, '2024-01-01', '9999-12-31', 3240),
    (8, 'PX_PRIMEFIXE_HTA', 'Prix prime fixe', 'BVE_HTA', 'HTA', NULL, '2024-01-01', '9999-12-31', 3240);

-- Redevance de comptage
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (9, 'ENT_CPT_TI', 'Entretien compteur avec rapport TI', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 2632),
    (10, 'ENT_CPT_MONO', 'Entretien compteur monophasé', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 539),
    (11, 'ENT_CPT_TRI_INF', 'Entretien compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 576),
    (12, 'ENT_CPT_TRI_SUP', 'Entretien compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 582),
    (13, 'LOC_CPT_TI', 'Location compteur avec rapport TI', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 0),
    (14, 'LOC_CPT_MONO', 'Location compteur monophasé', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 43),
    (15, 'LOC_CPT_TRI_INF', 'Location compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 105),
    (16, 'LOC_CPT_TRI_SUP', 'Location compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 89),
    (17, 'RED_CPT_CL05', 'Redevance comptage classe 0.5', NULL, 'HTA', NULL, '2024-01-01', '9999-12-31', 2523),
    (18, 'RED_CPT_CL1', 'Redevance comptage classe 1', NULL, 'HTA', NULL, '2024-01-01', '9999-12-31', 2632);

-- Frais
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (19, 'PX_DEPANNAGE_INJUS', 'Frais dépannage injustifié', NULL, NULL, NULL, '2024-01-01', '9999-12-31', 12124),
    (20, 'PX_COUPURE_IMPAYES', 'Frais Coupure pour impayés', NULL, NULL, NULL, '2024-01-01', '9999-12-31', 10416),
    (21, 'PX_ETALONNAGE_COMPTEUR', 'Frais Etalonnage compteur', NULL, NULL, NULL, '2024-01-01', '9999-12-31', 13887),
    (22, 'PX_PENALITE_FRAUDE', 'Pénalité pour Fraude', NULL, NULL, NULL, '2024-01-01', '9999-12-31', 35821);

-- Taxe communale
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (23, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', NULL, '2024-01-01', '9999-12-31', 9),
    (24, 'TAX_COM', 'Taux de la taxe communale',NULL, 'BT', '06', '2024-01-01', '9999-12-31', 7),
    (25, 'TAX_COM', 'Taux de la taxe communale',NULL, 'BT', '07', '2024-01-01', '9999-12-31', 7),
    (26, 'TAX_COM', 'Taux de la taxe communale',NULL, 'BT', '86', '2024-01-01', '9999-12-31', 7),
    (27, 'TAX_COM', 'Taux de la taxe communale',NULL, 'BT', '87',  '2024-01-01', '9999-12-31', 7),
    (28, 'TAX_COM', 'Taux de la taxe communale',NULL, 'HTA', NULL, '2024-01-01', '9999-12-31', 9),
    (29, 'TAX_COM', 'Taux de la taxe communale',NULL, 'HTA', '27', '2024-01-01', '9999-12-31', 7),
    (30, 'TAX_COM', 'Taux de la taxe communale',NULL, 'HTA', '26', '2024-01-01', '9999-12-31', 7);

-- Indice Borne Poste
INSERT INTO ct_grille_tarifaire
(id, code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    (31, 'PX_INDICE_BORNE_POSTE', 'Indice borne poste', 'BVE', 'HTA', NULL, '2024-01-01', '9999-12-31', 1.002);
