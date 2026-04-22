UPDATE crm_seller
SET cf_values = '{"cf_numero_piece_comptable": [{"long": 1}]}'
WHERE cf_values IS NULL OR cf_values->>'cf_ao_export_compta' IS NULL;
