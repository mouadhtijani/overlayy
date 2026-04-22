
--EP-176-update_ct_exploitation.sql
INSERT INTO ct_exploitations (Code_exploitation, Code_exploitation_ANAEL, Exploitation, Taux, Adresse, Tel, Tel_dépannage, Fax, E_mail, Site_Internet, Code_Journal)
VALUES
    ('01', '01', 'NOUMEA BT', 0.09, '15, rue Jean Chalier PK4 BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('02', '02', 'DUMBEA BT', 0.09, '15, rue Jean Chalier PK4 BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('03', '10', 'MONT DORE BT', 0.09, '147, rue du Grand Large BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('04', '08', 'BOURAIL BT', 0.09, '78, rue Simone Drémon BP 927 98870 BOURAIL', '44 11 10', '44 11 10', '44 19 64', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('05', '09', 'GOMEN BT', 0.09, 'Lotissement Siquiérios BP 44 98850 KOUMAC', '47 61 18', '47 61 18', '47 66 74', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('06', '06', 'KOUMAC BT', 0.07, 'Lotissement Siquiérios BP 44 98850 KOUMAC', '47 61 18', '47 61 18', '47 66 74', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('07', '04', 'LIFOU BT', 0.07, 'Route Territoriale 22 Lifou Waihmene BP 26 - 98820 WE', '45 12 33', '45 12 33', '45 11 20', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('08', '13', 'CANALA BT', 0.09, '1, rue Marcel Nonnaro 98813 CANALA', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('09', '14', 'THIO BT', 0.09, 'Route de Saint-Paul-Lot 3 Village 98829 THIO', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'BT'),
    ('21', '01', 'NOUMEA HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('22', '02', 'DUMBEA HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('23', '10', 'MT DORE HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('24', '08', 'BOURAIL HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('25', '09', 'GOMEN HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '45.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('26', '06', 'KOUMAC HT', 0.07, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('27', '04', 'LIFOU HT', 0.07, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('28', '13', 'CANALA HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05.36.36', '05.36.36', '46.35.00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('29', '14', 'THIO HT', 0.09, 'BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', 'HT'),
    ('83', '10', 'MONT DORE SOLAIRE', 0.09, '147, rue du Grand Large BP F3 98848 NOUMEA CEDEX', '46 36 36', '46 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('84', '08', 'BOURAIL SOLAIRE', 0.09, '78, rue Simone Drémon BP 927 98870 BOURAIL', '44 11 10', '44 11 10', '44 19 64', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('85', '09', 'GOMEN SOLAIRE', 0.09, 'Lotissement Siquiérios BP 44 98850 KOUMAC', '47 61 18', '47 61 18', '47 66 74', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('86', '06', 'KOUMAC SOLAIRE', 0.07, 'Lotissement Siquiérios BP 44 98850 KOUMAC', '47 61 18', '47 61 18', '47 66 74', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('87', '04', 'LIFOU SOLAIRE', 0.07, 'Route Territoriale 22 Lifou Waihmene BP 26 - 98820 WE', '45 12 33', '45 12 33', '45 11 20', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('88', '13', 'CANALA SOLAIRE', 0.09, '1, rue Marcel Nonnaro 98813 CANALA', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('89', '14', 'THIO SOLAIRE', 0.09, 'Route de Saint-Paul-Lot 3 Village 98829 THIO', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('91', '01', 'NOUMEA PROD. AUTONOMES', 0.09, '15, rue Jean Chalier PK4 BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('92', '02', 'DUMBEA PROD. AUTONOMES', 0.09, '15, rue Jean Chalier PK4 BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('93', '10', 'MONT DORE PROD. AUTONOMES', 0.09, '147, rue du Grand Large BP F3 98848 NOUMEA CEDEX', '05 36 36', '05 36 36', '46 35 00', 'clientele.eec@engie.com', 'www.eec.nc', NULL),
    ('97', '04', 'LIFOU PROD. AUTONOMES', 0.07, 'Route Territoriale 22 Lifou Waihmene BP 26 - 98820 WE', '45 12 33', '45 12 33', '45 11 20', 'clientele.eec@engie.com', 'www.eec.nc', NULL);



-- update_article.sql
update public.billing_accounting_article set description = 'Energie nuit EA (kWh)'where code ='AA_BVE_EA_NUIT';


--updateBillingSequence.sql
UPDATE public.billing_seq_invoice
SET sequence_size = 8;


--paramètres-comptables-au-niveau-des-charges.sql
UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104100',
    analytic_code_2 = 'E',
    analytic_code_3 = '000100'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '701011';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '0'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '443121';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104100',
    analytic_code_2 = 'E',
    analytic_code_3 = '000102'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '701012';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104100',
    analytic_code_2 = 'E',
    analytic_code_3 = '000102'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '701112';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104100',
    analytic_code_2 = 'E',
    analytic_code_3 = '000100'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '701111';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104100',
    analytic_code_2 = 'E',
    analytic_code_3 = '000125'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '701500';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '104500',
    analytic_code_2 = 'F',
    analytic_code_3 = '000187'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '708500';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '123000',
    analytic_code_2 = 'F',
    analytic_code_3 = '000174'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '706180';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '512500',
    analytic_code_2 = 'D3',
    analytic_code_3 = '000690'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '623800';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '103930',
    analytic_code_2 = 'D1'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '671100';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '0'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '445701';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '0'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '411112';

UPDATE public.billing_accounting_article baa
SET
    analytic_code_1 = '0'
FROM billing_accounting_code bac
WHERE baa.accounting_code_id = bac.id
  AND bac.code = '411111';


-- EP-128-update_script_cat.sql
update public.meveo_script_instance
set script_instance_cat_id= (select id from  public.meveo_script_instance_cat where code = 'INVOICE_VALIDATION')
where code like 'eec.epi.scripts.billing.ValidE%';


--EP-149-update_cf_numero_piece_comptable.sql
UPDATE crm_seller
SET cf_values = '{"cf_numero_piece_comptable": [{"long": 1}]}'
WHERE cf_values IS NULL OR cf_values->>'cf_ao_export_compta' IS NULL;

--EP_174 update description compte comptable.sql
UPDATE public.billing_accounting_code
SET description = 'Redevance comptage (BT)'
WHERE code = '701111';

UPDATE public.billing_accounting_code
SET description = 'Redevance comptage (HT)'
WHERE code = '701112';

--EP_175_add_tax_temp_3_6.sql
DELETE FROM billing_tax_mapping
WHERE tax_id IN (SELECT id FROM billing_tax WHERE code = 'TAX_3_CONSO')
  AND tax_category_id IN (SELECT id FROM billing_tax_category WHERE code = 'TAXCAT_ASSUJ')
  AND tax_class_id IN (SELECT id FROM billing_tax_class WHERE code = 'NORMAL');

INSERT INTO public.billing_tax_mapping (id, created, creator, tax_category_id, tax_class_id, tax_id,priority)
VALUES ( nextval('billing_tax_mapping_seq'), now(), 'opencell.admin',
         (SELECT id FROM billing_tax_category WHERE code = 'TAXCAT_ASSUJ'),
         (SELECT id FROM billing_tax_class WHERE code = 'NORMAL'),
         (SELECT id FROM billing_tax WHERE code = 'TAX_3_CONSO'),
         1);

DELETE FROM billing_tax_mapping
WHERE tax_id IN (SELECT id FROM billing_tax WHERE code = 'TAX_6_CONSO')
  AND tax_category_id IN (SELECT id FROM billing_tax_category WHERE code = 'TAXCAT_ASSUJ')
  AND tax_class_id IN (SELECT id FROM billing_tax_class WHERE code = 'NORMAL');

INSERT INTO public.billing_tax_mapping (id, created, creator, tax_category_id, tax_class_id, tax_id,priority)
VALUES ( nextval('billing_tax_mapping_seq'), now(), 'opencell.admin',
         (SELECT id FROM billing_tax_category WHERE code = 'TAXCAT_ASSUJ'),
         (SELECT id FROM billing_tax_class WHERE code = 'NORMAL'),
         (SELECT id FROM billing_tax WHERE code = 'TAX_6_CONSO'),
         1);
update ar_occ_template set manual_creation_enabled = 1
where code in ('PAY_DDT_TEMP','PAY_CHK','PAY_WIRE','INV_STD','TAX_TGC_3','TAX_TGC_6','TAX_TGC_0');


update public.ct_conso set calcul_HTA = true;
DELETE FROM billing_tax_mapping
WHERE id NOT IN (
    SELECT MIN(id)
    FROM billing_tax_mapping
    GROUP BY tax_category_id, tax_class_id, tax_id
);
commit;




-- EP-316  insert into ct_grille_tarifaire
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

update ct_grille_tarifaire set offre='OFFER_BORNE_VE_BT' where offre='BVE_BT';
update ct_grille_tarifaire set offre='OFFER_BORNE_VE_HTA' where offre='BVE_HTA';
update ct_grille_tarifaire set offre='OFFER_IRRIGATION' where offre='IRRIGATION';


delete from ct_grille_tarifaire where true;
INSERT INTO ct_grille_tarifaire
(code, description, offre, domaine_tension, code_exploitation, date_debut_validite, date_fin_validite, valeur)
VALUES
    ('ENT_CPT_MONO', 'Entretien compteur monophasé', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 539.000000000000),
    ('ENT_CPT_TI', 'Entretien compteur avec rapport TI', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 2632.000000000000),
    ('ENT_CPT_TRI_INF', 'Entretien compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 576.000000000000),
    ('ENT_CPT_TRI_SUP', 'Entretien compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 582.000000000000),
    ('LOC_CPT_MONO', 'Location compteur monophasé', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 43.000000000000),
    ('LOC_CPT_TI', 'Location compteur avec rapport TI', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 0.000000000000),
    ('LOC_CPT_TRI_INF', 'Location compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 105.000000000000),
    ('LOC_CPT_TRI_SUP', 'Location compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 89.000000000000),
    ('PX_COUPURE_IMPAYES', 'Frais Coupure pour impayés', NULL, NULL, NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 10416.000000000000),
    ('PX_DEPANNAGE_INJUS', 'Frais dépannage injustifié', NULL, NULL, NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 12124.000000000000),
    ('PX_EA_HC', 'Prix energie heures creuses', 'OFFER_IRRIGATION', 'BT', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 10.250000000000),
    ('PX_EA_HC', 'Prix energie heures creuses', 'OFFER_IRRIGATION', 'BT', NULL, '2022-01-01 00:00:00', '2023-04-30 00:00:00', 9.950000000000),
    ('PX_EA_HP', 'Prix energie heures pleines', 'OFFER_IRRIGATION', 'BT', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 30.740000000000),
    ('PX_EA_HP', 'Prix energie heures pleines', 'OFFER_IRRIGATION', 'BT', NULL, '2022-01-01 00:00:00', '2023-04-30 00:00:00', 29.840000000000),
    ('PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 8.240000000000),
    ('PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 8.240000000000),
    ('PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2022-01-01 00:00:00', '2023-04-30 00:00:00', 20.000000000000),
    ('PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 20.600000000000),
    ('PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2023-05-01 00:00:00', '9999-12-31 00:00:00', 20.600000000000),
    ('PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2022-01-01 00:00:00', '2023-04-30 00:00:00', 8.000000000000),
    ('PX_ETALONNAGE_COMPTEUR', 'Frais Etalonnage compteur', NULL, NULL, NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 13887.000000000000),
    ('PX_INDICE_BORNE_POSTE', 'Indice borne poste', 'BVE', 'HTA', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 1.002000000000),
    ('PX_PENALITE_FRAUDE', 'Pénalité pour Fraude', NULL, NULL, NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 35821.000000000000),
    ('PX_PRIMEFIXE_BT', 'Prix prime fixe', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 3240.000000000000),
    ('PX_PRIMEFIXE_HTA', 'Prix prime fixe', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 3240.000000000000),
    ('RED_CPT_CL05', 'Redevance comptage classe 0.5', NULL, 'HTA', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 2523.000000000000),
    ('RED_CPT_CL1', 'Redevance comptage classe 1', NULL, 'HTA', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 2632.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'BT', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 9.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '6', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '7', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '86', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '87', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', NULL, '2024-01-01 00:00:00', '9999-12-31 00:00:00', 9.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', '27', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000),
    ('TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', '26', '2024-01-01 00:00:00', '9999-12-31 00:00:00', 7.000000000000);


--occ_template
UPDATE ar_occ_template oct
SET accounting_code_id = NULL,
    account_code_client_side = NULL,
    contra_accounting_code_id = NULL,
    contra_accounting_code2_id = NULL
FROM billing_accounting_code bac
WHERE oct.accounting_code_id = bac.id
  AND bac.code IN ('419100000', '165000000', '701010000', '512010000', '658100000', '758100000', '411000000',
                   '654100000', '400000000', '401000000', '408000000', '410000000', '416000000', '418100000',
                   '445510000', '445660000', '445670000', '445710000', '487000000', '512000000', '512020000', '622000000',
                   '651100000', '701000000', '701020000', '511011000', '511002000', '511003000', '511000610', '531100000',
                   '511200000', '666000000', '766000000');
commit;

--articles
update billing_accounting_article article set tax_class_id = (
    select id from billing_tax_class where code in ('TAXC_TGC_CONSO_3')) where tax_class_id='-5';
update billing_accounting_article article set tax_class_id = (
    select id from billing_tax_class where code in ('TAXC_TGC_ZERO')) where tax_class_id='-3';
UPDATE billing_accounting_article article
SET accounting_code_id = NULL
FROM billing_accounting_code bac
WHERE article.accounting_code_id = bac.id
  AND bac.code IN ('419100000', '165000000', '701010000', '512010000', '658100000', '758100000', '411000000',
                   '654100000', '400000000', '401000000', '408000000', '410000000', '416000000', '418100000',
                   '445510000', '445660000', '445670000', '445710000', '487000000', '512000000', '512020000', '622000000',
                   '651100000', '701000000', '701020000', '511011000', '511002000', '511003000', '511000610', '531100000',
                   '511200000', '666000000', '766000000');
commit;


--tax
UPDATE billing_tax tax
SET accounting_code_id = NULL
FROM billing_accounting_code bac
WHERE tax.accounting_code_id = bac.id
  AND bac.code IN ('419100000', '165000000', '701010000', '512010000', '658100000', '758100000', '411000000',
                   '654100000', '400000000', '401000000', '408000000', '410000000', '416000000', '418100000',
                   '445510000', '445660000', '445670000', '445710000', '487000000', '512000000', '512020000', '622000000',
                   '651100000', '701000000', '701020000', '511011000', '511002000', '511003000', '511000610', '531100000',
                   '511200000', '666000000', '766000000');
commit;

--accounting code(code compta)
delete from billing_accounting_code ac where ac.code in('419100000','165000000','701010000','512010000','658100000','758100000','411000000',
                                                        '654100000','400000000','401000000','408000000','410000000','416000000','418100000',
                                                        '445510000','445660000','445670000','445710000','487000000','512000000','512020000','622000000',
                                                        '651100000','701000000','701020000','511011000','511002000','511003000','511000610',
                                                        '531100000','511200000','666000000','766000000');
commit;

delete
from ct_grille_tarifaire
where true;
INSERT INTO "ct_grille_tarifaire" ("id", "code", "description", "offre", "domaine_tension", "code_exploitation",
                                   "date_debut_validite", "date_fin_validite", "valeur")
VALUES (11, 'PX_EA_HC', 'Prix energie heures creuses', 'OFFER_IRRIGATION', 'BT', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 10.250000000000),
       (13, 'PX_EA_HP', 'Prix energie heures pleines', 'OFFER_IRRIGATION', 'BT', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 30.740000000000),
       (16, 'PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 8.240000000000),
       (19, 'PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 20.600000000000),
       (21, 'PX_ETALONNAGE_COMPTEUR', 'Frais Etalonnage compteur', NULL, NULL, NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 13887.000000000000),
       (22, 'PX_INDICE_BORNE_POSTE', 'Indice borne poste', 'BVE', 'HTA', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 1.002000000000),
       (23, 'PX_PENALITE_FRAUDE', 'Pénalité pour Fraude', NULL, NULL, NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 35821.000000000000),
       (24, 'PX_PRIMEFIXE_BT', 'Prix prime fixe', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 3240.000000000000),
       (26, 'RED_CPT_CL05', 'Redevance comptage classe 0.5', NULL, 'HTA', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 2523.000000000000),
       (27, 'RED_CPT_CL1', 'Redevance comptage classe 1', NULL, 'HTA', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 2632.000000000000),
       (1, 'ENT_CPT_MONO', 'Entretien compteur monophasé', NULL, 'BT', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 539.000000000000),
       (2, 'ENT_CPT_TI', 'Entretien compteur avec rapport TI', NULL, 'BT', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 2632.000000000000),
       (3, 'ENT_CPT_TRI_INF', 'Entretien compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL,
        '2000-01-01 00:00:00', '9999-12-31 00:00:00', 576.000000000000),
       (4, 'ENT_CPT_TRI_SUP', 'Entretien compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL,
        '2000-01-01 00:00:00', '9999-12-31 00:00:00', 582.000000000000),
       (5, 'LOC_CPT_MONO', 'Location compteur monophasé', NULL, 'BT', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 43.000000000000),
       (6, 'LOC_CPT_TI', 'Location compteur avec rapport TI', NULL, 'BT', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 0.000000000000),
       (7, 'LOC_CPT_TRI_INF', 'Location compteur triphasé gamme inférieure ou égale à 030/000', NULL, 'BT', NULL,
        '2000-01-01 00:00:00', '9999-12-31 00:00:00', 105.000000000000),
       (8, 'LOC_CPT_TRI_SUP', 'Location compteur triphasé gamme supérieure à 030/000', NULL, 'BT', NULL,
        '2000-01-01 00:00:00', '9999-12-31 00:00:00', 89.000000000000),
       (9, 'PX_COUPURE_IMPAYES', 'Frais Coupure pour impayés', NULL, NULL, NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 10416.000000000000),
       (10, 'PX_DEPANNAGE_INJUS', 'Frais dépannage injustifié', NULL, NULL, NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 12124.000000000000),
       (28, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', NULL, '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        9.000000000000),
       (29, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '6', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (30, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '7', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (31, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '86', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (32, 'TAX_COM', 'Taux de la taxe communale', NULL, 'BT', '87', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (33, 'TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', NULL, '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        9.000000000000),
       (34, 'TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', '27', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (35, 'TAX_COM', 'Taux de la taxe communale', NULL, 'HTA', '26', '2000-01-01 00:00:00', '9999-12-31 00:00:00',
        7.000000000000),
       (20, 'PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2000-01-01 00:00:00',
        '2023-04-30 00:00:00', 8.000000000000),
       (12, 'PX_EA_HC', 'Prix energie heures creuses', 'OFFER_IRRIGATION', 'BT', NULL, '2000-01-01 00:00:00',
        '2023-04-30 00:00:00', 9.950000000000),
       (14, 'PX_EA_HP', 'Prix energie heures pleines', 'OFFER_IRRIGATION', 'BT', NULL, '2000-01-01 00:00:00',
        '2023-04-30 00:00:00', 29.840000000000),
       (17, 'PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_BT', 'BT', NULL, '2000-01-01 00:00:00',
        '2023-04-30 00:00:00', 20.000000000000),
       (15, 'PX_EA_JOUR', 'Prix energie jour pour cadran EA', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 17.520000000000),
       (18, 'PX_EA_NUIT', 'Prix energie nuit pour cadran EA', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2023-05-01 00:00:00',
        '9999-12-31 00:00:00', 17.520000000000),
       (25, 'PX_PRIMEFIXE_HTA', 'Prix prime fixe', 'OFFER_BORNE_VE_HTA', 'HTA', NULL, '2000-01-01 00:00:00',
        '9999-12-31 00:00:00', 17869.000000000000);


