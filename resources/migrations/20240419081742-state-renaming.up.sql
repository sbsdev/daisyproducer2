UPDATE documents_state
SET name = "open"
WHERE name = "in_production";

--;;

UPDATE documents_state
SET name = "closed"
WHERE name = "finished";

--;;

UPDATE documents_document
SET state_id = 7
WHERE state_id = 1;


