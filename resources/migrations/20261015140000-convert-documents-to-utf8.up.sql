-- ISBNs are compared exactly from now on, so store them consistently
-- with an upper case X, as the queries do

UPDATE documents_document
SET source = UPPER(source)
WHERE BINARY source <> UPPER(source);

--;;

ALTER TABLE documents_document
CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_product
CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_state
CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_version
CONVERT TO CHARACTER SET utf8 COLLATE utf8_bin;
