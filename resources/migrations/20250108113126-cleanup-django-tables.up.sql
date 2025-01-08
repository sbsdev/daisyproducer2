-- Drop old, Django related tables that are no longer needed

DROP TABLE IF EXISTS south_migrationhistory;

--;;

DROP TABLE IF EXISTS django_migrations;

--;;

DROP TABLE IF EXISTS documents_state_responsible;

--;;

DROP TABLE IF EXISTS auth_user_groups;

--;;

DROP TABLE IF EXISTS auth_group_permissions;

--;;

DROP TABLE IF EXISTS auth_user_user_permissions;

--;;

DROP TABLE IF EXISTS auth_message;

--;;

DROP TABLE IF EXISTS auth_permission;

--;;

DROP TABLE IF EXISTS django_admin_log;

--;;

DROP TABLE IF EXISTS django_content_type;

--;;

DROP TABLE IF EXISTS django_site;

--;;

DROP TABLE IF EXISTS django_session;

--;;

DROP TABLE IF EXISTS documents_largeprintprofile;

--;;

DROP TABLE IF EXISTS documents_brailleprofile;

--;;

DROP TABLE IF EXISTS auth_group;

--;;

DROP TABLE IF EXISTS dictionary_importglobalword;

--;;

DROP TABLE IF EXISTS documents_state_next_states;

--;;

-- This is not really Django related, but attachments are no longer
-- used
DROP TABLE IF EXISTS documents_attachment;
