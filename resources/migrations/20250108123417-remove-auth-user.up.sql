-- Drop the references to the auth_user table and the auth_user table
-- itself, as users are now handled in LDAP

ALTER TABLE documents_version
DROP FOREIGN KEY IF EXISTS created_by_id_refs_id_1368ec995012716;

--;;

ALTER TABLE documents_version
DROP COLUMN IF EXISTS created_by_id;

--;;

ALTER TABLE documents_document
DROP FOREIGN KEY IF EXISTS assigned_to_id_refs_id_76390739499bcb48;

--;;

ALTER TABLE documents_document
DROP COLUMN IF EXISTS assigned_to_id;

--;;

DROP TABLE auth_user;

