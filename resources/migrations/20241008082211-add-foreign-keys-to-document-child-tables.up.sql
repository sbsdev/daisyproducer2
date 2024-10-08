-- Drop the old foreign key indexes
-- because they are not ON DELETE CASCADE
-- or because they do not conform to the naming scheme

ALTER TABLE dictionary_localword
DROP FOREIGN KEY IF EXISTS dictionary_localword_ibfk_1;

--;;

ALTER TABLE documents_version
DROP FOREIGN KEY IF EXISTS documents_version_documents_document;

--;;

ALTER TABLE documents_version
DROP FOREIGN KEY IF EXISTS document_id_refs_id_6e9aeb8bd76a8925;

--;;

ALTER TABLE documents_image
DROP FOREIGN KEY IF EXISTS document_id_refs_id_7dcae6cd2eb4b4cc;

--;;

ALTER TABLE documents_product
DROP FOREIGN KEY IF EXISTS document_id_refs_id_7ff5341caa42ce0c;

--;;

-- then add the new foreign keys

ALTER TABLE dictionary_unknownword
ADD CONSTRAINT fk_dictionary_unknownword__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id)
ON DELETE CASCADE;

--;;

ALTER TABLE dictionary_localword
ADD CONSTRAINT fk_dictionary_localword__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id)
ON DELETE CASCADE;

--;;

ALTER TABLE documents_version
ADD CONSTRAINT fk_documents_version__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id)
ON DELETE CASCADE;

--;;

ALTER TABLE documents_image
ADD CONSTRAINT fk_documents_image__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id)
ON DELETE CASCADE;

--;;

ALTER TABLE documents_product
ADD CONSTRAINT fk_documents_product__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id)
ON DELETE CASCADE;
