-- Convert empty `document.source` to NULL
UPDATE documents_document SET source = NULL WHERE source = '';

--;;

-- `document.source` should be unique
ALTER TABLE documents_document ADD CONSTRAINT UNIQUE document_source_unique (source);

