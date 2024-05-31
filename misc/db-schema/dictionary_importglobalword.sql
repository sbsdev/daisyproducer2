CREATE TABLE `dictionary_importglobalword` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `untranslated` varchar(100) COLLATE utf8_bin NOT NULL,
  `braille` varchar(100) COLLATE utf8_bin NOT NULL,
  `grade` smallint(5) unsigned NOT NULL,
  `type` smallint(5) unsigned NOT NULL DEFAULT '0',
  `homograph_disambiguation` varchar(100) COLLATE utf8_bin NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `dictionary_importglobalword_untranslated_1cc7ecdb5a975f5d_uniq` (`untranslated`,`type`,`grade`,`homograph_disambiguation`),
  KEY `dictionary_importglobalword_17609eb4` (`untranslated`),
  KEY `dictionary_importglobalword_6dff86b5` (`grade`),
  KEY `dictionary_importglobalword_f0bd6439` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_bin;
