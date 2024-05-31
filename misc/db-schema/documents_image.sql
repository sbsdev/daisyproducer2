CREATE TABLE documents_image (
  id INT PRIMARY KEY AUTO_INCREMENT,
  document_id INT NOT NULL,
  content varchar(100) NOT NULL,
  UNIQUE KEY (content, document_id),
  KEY `documents_image_f4226d13` (`document_id`),
  CONSTRAINT FOREIGN KEY (document_id) REFERENCES documents_document (id)
);
