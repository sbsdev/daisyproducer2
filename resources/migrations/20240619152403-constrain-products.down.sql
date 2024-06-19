ALTER TABLE documents_product DROP KEY product_document_id_identifier_unique;

--;;

ALTER TABLE documents_product DROP KEY product_identifier_unique;

--;;

ALTER TABLE documents_product
DROP INDEX product_document_id_type_partial_unique,
DROP COLUMN is_unique;
