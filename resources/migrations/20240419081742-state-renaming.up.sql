UPDATE documents_state
SET name = "open"
WHERE name = "in_production";

--;;

UPDATE documents_state
SET name = "closed"
WHERE name = "finished";


