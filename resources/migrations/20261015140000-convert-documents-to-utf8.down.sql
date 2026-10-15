ALTER TABLE documents_document
MODIFY `subject` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `description` longtext CHARACTER SET latin1,
MODIFY `publisher` varchar(255) CHARACTER SET latin1 NOT NULL DEFAULT 'SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte',
MODIFY `identifier` varchar(255) CHARACTER SET latin1 NOT NULL,
MODIFY `source` varchar(20) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `language` varchar(10) CHARACTER SET latin1 NOT NULL,
MODIFY `rights` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `source_edition` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `source_publisher` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `source_rights` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `production_series` varchar(25) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `production_series_number` varchar(25) CHARACTER SET latin1 DEFAULT NULL,
MODIFY `production_source` varchar(25) CHARACTER SET latin1 DEFAULT NULL;

--;;

ALTER TABLE documents_product
MODIFY `identifier` varchar(255) CHARACTER SET latin1 COLLATE latin1_german1_ci NOT NULL;

--;;

ALTER TABLE documents_state
MODIFY `name` varchar(32) CHARACTER SET latin1 NOT NULL;

--;;

ALTER TABLE documents_version
MODIFY `comment` varchar(255) CHARACTER SET latin1 NOT NULL,
MODIFY `content` varchar(100) CHARACTER SET latin1 NOT NULL,
MODIFY `created_by` varchar(32) CHARACTER SET latin1 NOT NULL;
