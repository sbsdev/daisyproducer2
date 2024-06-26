ALTER TABLE documents_document
MODIFY subject VARCHAR(255) NOT NULL,
MODIFY description LONGTEXT NOT NULL,
MODIFY source VARCHAR(20) NOT NULL,
MODIFY rights VARCHAR(255) NOT NULL,
MODIFY source_edition VARCHAR(255) NOT NULL,
MODIFY source_publisher VARCHAR(255) NOT NULL,
MODIFY source_rights VARCHAR(255) NOT NULL,
MODIFY production_series VARCHAR(25) NOT NULL,
MODIFY production_series_number VARCHAR(25) NOT NULL,
MODIFY production_source VARCHAR(25) NOT NULL,
MODIFY created_at DATETIME,
MODIFY modified_at DATETIME,
ALTER state_id DROP DEFAULT;
