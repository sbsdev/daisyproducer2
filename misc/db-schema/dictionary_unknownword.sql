CREATE TABLE dictionary_unknownword (
  untranslated varchar(100) NOT NULL,
  type SMALLINT(5) UNSIGNED NOT NULL DEFAULT 0,
  homograph_disambiguation varchar(100) NOT NULL,
  isLocal tinyint(1) NOT NULL DEFAULT 0,
  isIgnored tinyint(1) NOT NULL DEFAULT 0,
  document_id INT NOT NULL,
  KEY `dictionary_unknownword_index` (`document_id`,`untranslated`)
);
