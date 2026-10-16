-- The statistics prevented a document from being deleted. Instead of
-- deleting them with the document, keep them without a document, as they
-- are meant to show how the share of unknown words develops over the years

ALTER TABLE statistics_documentstatistic
DROP FOREIGN KEY fk_statistics_documentstatistic__documents_document;

--;;

ALTER TABLE statistics_documentstatistic
MODIFY document_id int(11) DEFAULT NULL;

--;;

ALTER TABLE statistics_documentstatistic
ADD CONSTRAINT fk_statistics_documentstatistic__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id) ON DELETE SET NULL;
