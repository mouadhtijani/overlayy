-----------------------------[R006]------------------------------------

--PreDeploy/R006/EP-128-update_script_cat.sql
update public.meveo_script_instance
set script_instance_cat_id= (select id from  public.meveo_script_instance_cat where code = 'INVOICE_VALIDATION')
where code like 'eec.epi.scripts.billing.ValidE%';
commit;

-----------------------------[R007]------------------------------------

--PreDeploy/R007/EP_175_add_tax_temp_3_6.sql
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

-----------------------------[R011]------------------------------------

--PreDeploy/R011/EP-165_delete_title_id_from_Cus.sql
update crm_customer set title_id= null;
update ar_customer_account set title_id= null;
update billing_billing_account set title_id= null;
update billing_user_account set title_id= null;

