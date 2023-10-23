ALTER TABLE statistics_documentstatistic RENAME statistics_documentstatistic_old;
--;;
CREATE TABLE statistics_documentstatistic (
  id int(11) NOT NULL AUTO_INCREMENT PRIMARY KEY,
  date datetime NOT NULL,
  document_id int(11) NOT NULL,
  total int(10) unsigned NOT NULL,
  unknown int(10) unsigned NOT NULL,
  UNIQUE KEY (document_id),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES documents_document(id)
);
--;;
INSERT INTO statistics_documentstatistic (id, date, document_id, total, unknown)
SELECT DISTINCT id, date, document_id, total, MAX(unknown) AS unknown
FROM statistics_documentstatistic_old
GROUP BY document_id;

