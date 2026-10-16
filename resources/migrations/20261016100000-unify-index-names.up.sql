-- Give all keys and constraints names following one scheme:
-- <table>_<columns>_unique, <table>_<columns>_index and fk_<table>__<target>.
-- The names stem from Django, from MariaDB (unnamed keys) and from our own
-- migrations. Each table is changed in one statement, so that no uniqueness
-- constraint is missing in between.

-- The separate indexes on untranslated are dropped: the unique keys start
-- with untranslated and serve the same queries
ALTER TABLE dictionary_globalword
DROP INDEX dictionary_localword_uniq,
ADD CONSTRAINT UNIQUE dictionary_globalword_unique (untranslated, type, homograph_disambiguation),
DROP INDEX untranslated;

--;;

ALTER TABLE dictionary_localword
DROP INDEX dictionary_localword_uniq,
ADD CONSTRAINT UNIQUE dictionary_localword_unique (untranslated, type, homograph_disambiguation, document_id),
DROP INDEX untranslated,
DROP INDEX document_id,
ADD INDEX dictionary_localword_document_id_index (document_id);

--;;

-- A foreign key cannot be renamed, so add the new one before dropping the old
ALTER TABLE documents_document
ADD CONSTRAINT fk_documents_document__documents_state FOREIGN KEY (state_id) REFERENCES documents_state (id);

--;;

ALTER TABLE documents_document
DROP FOREIGN KEY state_id_refs_id_1a873183ef7997da;

--;;

ALTER TABLE documents_document
DROP INDEX identifier,
ADD CONSTRAINT UNIQUE documents_document_identifier_unique (identifier),
DROP INDEX document_source_unique,
ADD CONSTRAINT UNIQUE documents_document_source_unique (source),
DROP INDEX documents_document_state_id,
ADD INDEX documents_document_state_id_index (state_id);

--;;

ALTER TABLE documents_image
DROP INDEX documents_image_content_635708861a13c14f_uniq,
ADD CONSTRAINT UNIQUE documents_image_content_document_id_unique (content, document_id),
DROP INDEX documents_image_f4226d13,
ADD INDEX documents_image_document_id_index (document_id);

--;;

ALTER TABLE documents_product
DROP INDEX product_identifier_unique,
ADD CONSTRAINT UNIQUE documents_product_identifier_unique (identifier),
DROP INDEX product_document_id_type_partial_unique,
ADD CONSTRAINT UNIQUE documents_product_document_id_type_partial_unique (is_unique, type, document_id),
DROP INDEX documents_product_f4226d13,
ADD INDEX documents_product_document_id_index (document_id);

--;;

ALTER TABLE documents_state
DROP INDEX name,
ADD CONSTRAINT UNIQUE documents_state_name_unique (name);

--;;

ALTER TABLE documents_version
DROP INDEX documents_version_document_id,
ADD INDEX documents_version_document_id_index (document_id);

--;;

ALTER TABLE statistics_documentstatistic
ADD CONSTRAINT fk_statistics_documentstatistic__documents_document FOREIGN KEY (document_id) REFERENCES documents_document (id);

--;;

ALTER TABLE statistics_documentstatistic
DROP FOREIGN KEY document_id_refs_id_544d62dcd2388120;

--;;

ALTER TABLE statistics_documentstatistic
DROP INDEX statistics_documentstatistic_f4226d13,
ADD INDEX statistics_documentstatistic_document_id_index (document_id);
