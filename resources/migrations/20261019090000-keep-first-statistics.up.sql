-- The old system recorded the statistics every time words were confirmed.
-- Only the first entry per document and grade, i.e. the state when the book
-- arrived, is of interest (#94). Keep the earliest one (the lower id if two
-- have the same date) and make sure there is only ever one from now on.
-- Statistics of deleted documents (document_id NULL) are kept as they are.

DELETE later
FROM statistics_documentstatistic later
JOIN statistics_documentstatistic earlier
  ON earlier.document_id = later.document_id
 AND earlier.grade = later.grade
 AND (earlier.date < later.date OR (earlier.date = later.date AND earlier.id < later.id));

--;;

-- the unique key starts with document_id and thus also serves the foreign key
ALTER TABLE statistics_documentstatistic
ADD CONSTRAINT UNIQUE statistics_documentstatistic_document_id_grade_unique (document_id, grade),
DROP INDEX statistics_documentstatistic_document_id_index;
