-- Fill `created_by` with the values that correspond to `created_by_id`
UPDATE documents_version
INNER JOIN auth_user
ON documents_version.created_by_id = auth_user.id
SET created_by = auth_user.username
WHERE documents_version.created_by IS NULL;

--;;

-- `created_by` should not be NULL
ALTER TABLE documents_version
MODIFY created_by VARCHAR(32) NOT NULL;

