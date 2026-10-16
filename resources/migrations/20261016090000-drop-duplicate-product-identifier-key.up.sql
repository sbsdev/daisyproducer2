-- Django created a unique key on the identifier, named after the column.
-- product_identifier_unique, added later, enforces the same, so drop the old one.

ALTER TABLE documents_product
DROP INDEX identifier;
