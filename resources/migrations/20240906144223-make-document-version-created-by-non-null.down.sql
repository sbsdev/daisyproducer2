-- allow `created_by` to be NULL
ALTER TABLE documents_version
MODIFY created_by VARCHAR(32) DEFAULT NULL;
