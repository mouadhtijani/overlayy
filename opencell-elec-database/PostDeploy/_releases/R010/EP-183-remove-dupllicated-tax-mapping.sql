DELETE FROM billing_tax_mapping
WHERE id NOT IN (
    SELECT MIN(id)
    FROM billing_tax_mapping
    GROUP BY tax_category_id, tax_class_id, tax_id
);