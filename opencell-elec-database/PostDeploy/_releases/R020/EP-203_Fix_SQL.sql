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

