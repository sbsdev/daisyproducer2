-- allow NULL values for values that are often not known,
-- let the database handle the created_at and modified_at fields,
-- and set the default state_id to the initial state
ALTER TABLE documents_document
MODIFY subject VARCHAR(255) NULL,
MODIFY description LONGTEXT NULL,
MODIFY source VARCHAR(20) NULL,
MODIFY rights VARCHAR(255) NULL,
MODIFY source_edition VARCHAR(255) NULL,
MODIFY source_publisher VARCHAR(255) NULL,
MODIFY source_rights VARCHAR(255) NULL,
MODIFY production_series VARCHAR(25) NULL,
MODIFY production_series_number VARCHAR(25) NULL,
MODIFY production_source VARCHAR(25) NULL,
MODIFY created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
MODIFY modified_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
ALTER publisher SET DEFAULT "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte",
ALTER state_id SET DEFAULT 7;
