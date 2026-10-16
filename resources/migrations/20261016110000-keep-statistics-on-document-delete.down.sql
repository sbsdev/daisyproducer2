ALTER TABLE statistics_documentstatistic
DROP FOREIGN KEY fk_statistics_documentstatistic__documents_document;

--;;

-- the column cannot be made NOT NULL again while it contains statistics of
-- deleted documents
DELETE FROM statistics_documentstatistic WHERE document_id IS NULL;

--;;

ALTER TABLE statistics_documentstatistic
MODIFY document_id int(11) NOT NULL;

--;;

ALTER TABLE statistics_documentstatistic
ADD CONSTRAINT fk_statistics_documentstatistic__documents_document
FOREIGN KEY (document_id) REFERENCES documents_document (id);
