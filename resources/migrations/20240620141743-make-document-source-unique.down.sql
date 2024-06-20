ALTER TABLE documents_document DROP KEY document_source_unique;

--;;

UPDATE documents_document SET source = '' WHERE source IS NULL;
