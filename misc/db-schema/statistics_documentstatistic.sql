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
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci;
