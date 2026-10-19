-- The deleted statistics cannot be restored, only the indexes

ALTER TABLE statistics_documentstatistic
ADD INDEX statistics_documentstatistic_document_id_index (document_id),
DROP INDEX statistics_documentstatistic_document_id_grade_unique;
