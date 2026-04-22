UPDATE billing_subscription
SET cf_values = jsonb_set(cf_values, '{cf_intensite_limiteur, 0}', concat('{"long": ', cf_values -> 'cf_intensite_limiteur' -> 0 ->> 'string', '}')::jsonb)
WHERE cf_values -> 'cf_intensite_limiteur' -> 0 ->> 'string' IS NOT NULL;

UPDATE billing_subscription
SET cf_values = jsonb_set(cf_values, '{cf_puissance_reservee, 0}', concat('{"long": ', cf_values -> 'cf_puissance_reservee' -> 0 ->> 'string', '}')::jsonb)
WHERE cf_values -> 'cf_puissance_reservee' -> 0 ->> 'string' IS NOT NULL;