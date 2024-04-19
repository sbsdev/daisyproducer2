UPDATE documents_state
SET name = "in_production"
WHERE name = "open";

--;;

UPDATE documents_state
SET name = "finished"
WHERE name = "closed";


