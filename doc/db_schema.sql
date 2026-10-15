-- Schema of the daisyproducer2 database (generated with SHOW CREATE TABLE)

CREATE TABLE `dictionary_globalword` (
  `untranslated` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `uncontracted` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `contracted` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin DEFAULT NULL,
  `type` smallint(6) unsigned NOT NULL DEFAULT '0',
  `homograph_disambiguation` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL DEFAULT '',
  UNIQUE KEY `dictionary_localword_uniq` (`untranslated`,`type`,`homograph_disambiguation`),
  KEY `untranslated` (`untranslated`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
  CONSTRAINT `fk_dictionary_localword__documents_document` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `dictionary_unknownword` (
  `untranslated` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `type` smallint(5) unsigned NOT NULL DEFAULT '0',
  `homograph_disambiguation` varchar(100) CHARACTER SET utf8 COLLATE utf8_bin NOT NULL,
  `isLocal` tinyint(1) NOT NULL DEFAULT '0',
  `isIgnored` tinyint(1) NOT NULL DEFAULT '0',
  `document_id` int(11) NOT NULL,
  KEY `dictionary_unknownword_document_id_untranslated_index` (`document_id`,`untranslated`),
  CONSTRAINT `fk_dictionary_unknownword__documents_document` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `documents_document` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(255) COLLATE utf8_bin NOT NULL,
  `author` varchar(255) COLLATE utf8_bin DEFAULT NULL,
  `subject` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `description` longtext CHARACTER SET latin1,
  `publisher` varchar(255) CHARACTER SET latin1 NOT NULL DEFAULT 'SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte',
  `date` date NOT NULL,
  `identifier` varchar(255) CHARACTER SET latin1 NOT NULL,
  `source` varchar(20) CHARACTER SET latin1 DEFAULT NULL,
  `language` varchar(10) CHARACTER SET latin1 NOT NULL,
  `rights` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `source_date` date DEFAULT NULL,
  `source_edition` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `source_publisher` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `source_rights` varchar(255) CHARACTER SET latin1 DEFAULT NULL,
  `state_id` int(11) NOT NULL DEFAULT '7',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `modified_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `production_series` varchar(25) CHARACTER SET latin1 DEFAULT NULL,
  `production_series_number` varchar(25) CHARACTER SET latin1 DEFAULT NULL,
  `production_source` varchar(25) CHARACTER SET latin1 DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `identifier` (`identifier`),
  UNIQUE KEY `document_source_unique` (`source`),
  KEY `documents_document_state_id` (`state_id`),
  CONSTRAINT `state_id_refs_id_1a873183ef7997da` FOREIGN KEY (`state_id`) REFERENCES `documents_state` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `documents_image` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `document_id` int(11) NOT NULL,
  `content` varchar(100) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `documents_image_content_635708861a13c14f_uniq` (`content`,`document_id`),
  KEY `documents_image_f4226d13` (`document_id`),
  CONSTRAINT `fk_documents_image__documents_document` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `documents_product` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `identifier` varchar(255) CHARACTER SET latin1 COLLATE latin1_german1_ci NOT NULL,
  `type` smallint(5) unsigned NOT NULL,
  `document_id` int(11) NOT NULL,
  `is_unique` tinyint(1) AS (CASE WHEN type <> 0 THEN TRUE ELSE NULL END) PERSISTENT,
  PRIMARY KEY (`id`),
  UNIQUE KEY `identifier` (`identifier`),
  UNIQUE KEY `product_identifier_unique` (`identifier`),
  UNIQUE KEY `product_document_id_type_partial_unique` (`is_unique`,`type`,`document_id`),
  KEY `documents_product_f4226d13` (`document_id`),
  CONSTRAINT `fk_documents_product__documents_document` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `documents_state` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET latin1 NOT NULL,
  `sort_order` smallint(5) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE TABLE `documents_version` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `comment` varchar(255) CHARACTER SET latin1 NOT NULL,
  `document_id` int(11) NOT NULL,
  `content` varchar(100) CHARACTER SET latin1 NOT NULL,
  `created_by` varchar(32) CHARACTER SET latin1 NOT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `documents_version_document_id` (`document_id`),
  CONSTRAINT `fk_documents_version__documents_document` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;

CREATE ALGORITHM=UNDEFINED DEFINER=`daisyproducer`@`w302.sbszh.ch` SQL SECURITY DEFINER VIEW `hyphenation_words` AS select `hyphenation`.`words`.`word` AS `word`,`hyphenation`.`words`.`hyphenation` AS `hyphenation`,`hyphenation`.`words`.`spelling` AS `spelling` from `hyphenation`.`words`;

CREATE TABLE `schema_migrations` (
  `id` bigint(20) NOT NULL,
  `applied` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `description` varchar(1024) DEFAULT NULL,
  UNIQUE KEY `id` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE `statistics_documentstatistic` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `date` datetime NOT NULL,
  `document_id` int(11) NOT NULL,
  `grade` smallint(5) unsigned NOT NULL,
  `total` int(10) unsigned NOT NULL,
  `unknown` int(10) unsigned NOT NULL,
  PRIMARY KEY (`id`),
  KEY `statistics_documentstatistic_f4226d13` (`document_id`),
  CONSTRAINT `document_id_refs_id_544d62dcd2388120` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
