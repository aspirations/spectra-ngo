-- Staff-store products with no selling price yet get a default of 100; priced items are left alone.
UPDATE shelter_products
SET unit_price = 100
WHERE staff_sale IS TRUE
  AND unit_price = 0;
