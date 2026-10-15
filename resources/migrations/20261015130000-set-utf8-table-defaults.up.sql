-- Only change the defaults for new columns, the existing columns keep their charset

ALTER TABLE documents_document
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_image
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_product
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_state
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE documents_version
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;

--;;

ALTER TABLE statistics_documentstatistic
DEFAULT CHARACTER SET utf8 COLLATE utf8_bin;
