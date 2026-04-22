    -- Part Variable :

    INSERT INTO public.ct_grille_tarifaire
    (code, description, offre, domaine_tension, poste_horosaisonnier, nature_mesure, transformateur_intensite, phasage_comptage, gamme_compteur, date_debut_validite, date_fin_validite, valeur)
    VALUES
        ('PX_BVE_EA_JOUR', 'Prix energie jour pour cadran EA', 'BVE', '', 'JOUR', 'EA', '', '', '', '2023-01-01', '9999-12-31', 8.24),
        ('PX_BVE_EA_NUIT', 'Prix energie nuit pour cadran EA', 'BVE', '', 'NUIT', 'EA', '', '', '', '2023-01-01', '9999-12-31', 20.60),
        ('PX_BVE_PRIMEFIXE', 'Prix prime fixe', 'BVE', '', '', '', '', '', '', '2023-01-01', '9999-12-31', 270),
        ('PX_IR_HP', 'Prix energie heures pleines', 'IRRIGATION', '', 'HP', 'EA', '', '', '', '2023-01-01', '9999-12-31', 30.74),
        ('PX_IR_HC', 'Prix energie heures creuses', 'IRRIGATION', '', 'HC', 'EA', '', '', '', '2023-01-01', '9999-12-31', 10.25),
        ('PX_PRIME_TRANSFO', 'Prix prime de transformation', 'BVE', 'HTA', '', '', '', '', '', '2023-01-01', '9999-12-31', 577),
        ('PX_PRIME_RACC', 'Prix prime de raccordement', 'BVE', 'HTA', '', '', '', '', '', '2023-01-01', '9999-12-31', 0.05),
        ('PX_INDICE_BORNE_POSTE', 'Indice borne poste', 'BVE', 'HTA', '', '', '', '', '', '2023-01-01', '9999-12-31', 1.002);


    -- Part fixe :

    INSERT INTO public.ct_grille_tarifaire
    (code, description, offre, domaine_tension, poste_horosaisonnier, nature_mesure, transformateur_intensite, phasage_comptage, gamme_compteur, date_debut_validite, date_fin_validite, valeur)
    VALUES
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'BT', '', '', 'Oui', 'TRIPHASE', '-', '2023-01-01', '9999-12-31', 2632),
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'BT', '', '', 'Non', 'TRIPHASE', '30/90', '2023-01-01', '9999-12-31', 671),
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'BT', '', '', 'Non', 'TRIPHASE', '-', '2023-01-01', '9999-12-31', 681),
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'BT', '', '', 'Non', 'Monophase', '5/15, 15/45, 15/60', '2023-01-01', '9999-12-31', 582),
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'HTA', '', '', '', 'Classe 1', '', '2023-01-01', '9999-12-31', 2632),
        ('PX_RED_COMPTAGE', 'Prix redevance de comptage', '', 'HTA', '', '', '', 'Classe 0,5', '', '2023-01-01', '9999-12-31', 2523);

    -- Frais :

    INSERT INTO public.ct_grille_tarifaire
    (code, description, offre, domaine_tension, poste_horosaisonnier, nature_mesure, transformateur_intensite, phasage_comptage, gamme_compteur, date_debut_validite, date_fin_validite, valeur)
    VALUES
        ('PX_DEPANNAGE_INJUS', 'Frais dépannage injustifié', '', '', '', '', '', '', '', '2023-01-01', '9999-12-31', 12124),
        ('PX_COUPURE_IMPAYES', 'Frais Coupure pour impayés', '', '', '', '', '', '', '', '2023-01-01', '9999-12-31', 10416),
        ('PX_ETALONNAGE_COMPTEUR', 'Frais Etalonnage compteur', '', '', '', '', '', '', '', '2023-01-01', '9999-12-31', 13887),
        ('PX_PENALITE_FRAUDE', 'Pénalité pour Fraude', '', '', '', '', '', '', '', '2023-01-01', '9999-12-31', 35821);
