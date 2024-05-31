CREATE TABLE dictionary_globalword (
  untranslated varchar(100) NOT NULL,
  uncontracted varchar(100),
  contracted varchar(100),
  type SMALLINT(6) UNSIGNED NOT NULL DEFAULT 0,
  homograph_disambiguation varchar(100),
  UNIQUE KEY (untranslated, type, homograph_disambiguation),
  KEY `untranslated` (`untranslated`)
);
