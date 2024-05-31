CREATE TABLE `dictionary_localword` (
  `untranslated` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `uncontracted` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `contracted` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `type` smallint(6) unsigned NOT NULL DEFAULT '0',
  `homograph_disambiguation` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '',
  `document_id` int(11) NOT NULL,
  `isLocal` tinyint(1) NOT NULL DEFAULT '0',
  `isConfirmed` tinyint(1) NOT NULL DEFAULT '0',
  UNIQUE KEY `dictionary_localword_uniq` (`untranslated`,`type`,`homograph_disambiguation`,`document_id`),
  KEY `untranslated` (`untranslated`),
  KEY `document_id` (`document_id`),
  CONSTRAINT `dictionary_localword_ibfk_1` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
