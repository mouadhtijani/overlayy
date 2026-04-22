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