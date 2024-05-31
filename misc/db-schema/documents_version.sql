CREATE TABLE documents_version (
  id INT PRIMARY KEY AUTO_INCREMENT,
  comment VARCHAR(255) NOT NULL,
  document_id INT NOT NULL,
  content VARCHAR(100) NOT NULL,
  created_by VARCHAR(32) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `documents_version_document_id` (`document_id`),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES documents_document (id) ON DELETE CASCADE
);
