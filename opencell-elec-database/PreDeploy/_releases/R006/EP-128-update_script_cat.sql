update public.meveo_script_instance
set script_instance_cat_id= (select id from  public.meveo_script_instance_cat where code = 'INVOICE_VALIDATION')
where code like 'eec.epi.scripts.billing.ValidE%';
commit;