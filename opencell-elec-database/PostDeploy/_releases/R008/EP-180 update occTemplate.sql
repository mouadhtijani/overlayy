update ar_occ_template set manual_creation_enabled = 1
where code in ('PAY_DDT_TEMP','PAY_CHK','PAY_WIRE','INV_STD','TAX_TGC_3','TAX_TGC_6','TAX_TGC_0');
commit;