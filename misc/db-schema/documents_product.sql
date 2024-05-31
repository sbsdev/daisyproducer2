CREATE TABLE `documents_product` (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `identifier` varchar(255) COLLATE latin1_german1_ci NOT NULL,
  `type` smallint(5) unsigned NOT NULL,
  `document_id` int(11) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `identifier` (`identifier`),
  KEY `documents_product_f4226d13` (`document_id`),
  CONSTRAINT `document_id_refs_id_7ff5341caa42ce0c` FOREIGN KEY (`document_id`) REFERENCES `documents_document` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_german1_ci;
