ALTER TABLE statistics_documentstatistic
DROP INDEX statistics_documentstatistic_document_id_index,
ADD INDEX statistics_documentstatistic_f4226d13 (document_id);

--;;

ALTER TABLE statistics_documentstatistic
ADD CONSTRAINT document_id_refs_id_544d62dcd2388120 FOREIGN KEY (document_id) REFERENCES documents_document (id);

--;;

ALTER TABLE statistics_documentstatistic
DROP FOREIGN KEY fk_statistics_documentstatistic__documents_document;

--;;

ALTER TABLE documents_version
DROP INDEX documents_version_document_id_index,
ADD INDEX documents_version_document_id (document_id);

--;;

ALTER TABLE documents_state
DROP INDEX documents_state_name_unique,
ADD CONSTRAINT UNIQUE name (name);

--;;

ALTER TABLE documents_product
DROP INDEX documents_product_identifier_unique,
ADD CONSTRAINT UNIQUE product_identifier_unique (identifier),
DROP INDEX documents_product_document_id_type_partial_unique,
ADD CONSTRAINT UNIQUE product_document_id_type_partial_unique (is_unique, type, document_id),
DROP INDEX documents_product_document_id_index,
ADD INDEX documents_product_f4226d13 (document_id);

--;;

ALTER TABLE documents_image
DROP INDEX documents_image_content_document_id_unique,
ADD CONSTRAINT UNIQUE documents_image_content_635708861a13c14f_uniq (content, document_id),
DROP INDEX documents_image_document_id_index,
ADD INDEX documents_image_f4226d13 (document_id);

--;;

ALTER TABLE documents_document
DROP INDEX documents_document_identifier_unique,
ADD CONSTRAINT UNIQUE identifier (identifier),
DROP INDEX documents_document_source_unique,
ADD CONSTRAINT UNIQUE document_source_unique (source),
DROP INDEX documents_document_state_id_index,
ADD INDEX documents_document_state_id (state_id);

--;;

ALTER TABLE documents_document
ADD CONSTRAINT state_id_refs_id_1a873183ef7997da FOREIGN KEY (state_id) REFERENCES documents_state (id);

--;;

ALTER TABLE documents_document
DROP FOREIGN KEY fk_documents_document__documents_state;

--;;

ALTER TABLE dictionary_localword
DROP INDEX dictionary_localword_unique,
ADD CONSTRAINT UNIQUE dictionary_localword_uniq (untranslated, type, homograph_disambiguation, document_id),
ADD INDEX untranslated (untranslated),
DROP INDEX dictionary_localword_document_id_index,
ADD INDEX document_id (document_id);

--;;

ALTER TABLE dictionary_globalword
DROP INDEX dictionary_globalword_unique,
ADD CONSTRAINT UNIQUE dictionary_localword_uniq (untranslated, type, homograph_disambiguation),
ADD INDEX untranslated (untranslated);
