-- add a plain created_by field
-- since we now keep the auth information in LDAP we no longer want to
-- have the created_by field to be a reference to the auth_user table
-- we allow null values to be backwards compatible. When the Python
-- code is gone we can tighten this to only allow non-null values
ALTER TABLE documents_version
ADD COLUMN IF NOT EXISTS created_by varchar(32)
AFTER content;

--;;

-- set a default for the created_by_id field (which we are keeping for
-- backwards compatibility)
ALTER TABLE documents_version
MODIFY created_by_id int(11) NOT NULL DEFAULT 1;

--;;

-- set a default for the created_at field so that the db adds it on insert
ALTER TABLE documents_version
MODIFY created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP;



