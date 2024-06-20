-- (`product.document_id`, `product.identifier`) should be unique
ALTER TABLE documents_product ADD CONSTRAINT UNIQUE product_document_id_identifier_unique (document_id, identifier);

--;;

-- `product.identifier` should be unique
ALTER TABLE documents_product ADD CONSTRAINT UNIQUE product_identifier_unique (identifier);

--;;

-- (`product.type`, `product.document_id`) should be unique, except for type 0
-- really we want a partial index here, but that exists only in real databases so
-- we have to resort to a workaround by using a virtual fake column, see
-- https://stackoverflow.com/q/7804565 and in particular
-- https://dba.stackexchange.com/a/167552
ALTER TABLE documents_product
ADD COLUMN is_unique BOOLEAN AS (CASE WHEN type <> 0 THEN TRUE ELSE NULL END) PERSISTENT,
ADD CONSTRAINT UNIQUE product_document_id_type_partial_unique (is_unique, type, document_id);
