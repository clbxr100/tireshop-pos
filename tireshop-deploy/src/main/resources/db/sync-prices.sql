-- Script to synchronize price and sellingPrice in existing product records

-- Update price from sellingPrice where price is null but sellingPrice is not
UPDATE products 
SET price = sellingPrice 
WHERE price IS NULL AND sellingPrice IS NOT NULL;

-- Update sellingPrice from price where sellingPrice is null but price is not
UPDATE products 
SET sellingPrice = price 
WHERE sellingPrice IS NULL AND price IS NOT NULL;

-- Set both to 0 where both are null
UPDATE products 
SET price = 0, sellingPrice = 0
WHERE price IS NULL AND sellingPrice IS NULL; 