-- Convert empty `document.source` to NULL
UPDATE documents_document SET source = NULL WHERE source = '';

--;;

-- We had some lengthy discussions about this: There are some rare valid cases where an
-- ISBN is not unique for a document. Out of the 20k documents that we have been producing
-- so far we had around 7 documents where the ISBN was a duplicate:
-- 1) Teaching material sometimes comes with a bundle, the book accompanied by a students
--    workbook. Since the whole thing comes as a bundle it only has one ISBN
-- 2) Teaching material keeps the same ISBN for a new revision

-- We could handle all cases if the index was over (source, source_edition, title). That
-- just seems to promiscuous. Also it doesn't really help with the import from ABACUS. The
-- product would end up with the wrong document.

-- Maybe the index could be (source, source_edition). Then we could handle the case where
-- they keep the ISBN for a new edition. That also seems wrong conceptionally. It might
-- help with the ABACUS import.

-- In the end I think we should stick with the most restrictive constraint. If we ever
-- encounter these anomalies we can find a solution for them then, either by relaxing the
-- constraint or by dropping the non-unique ISBN in ABACUS.

-- The `document.source` should be unique
ALTER TABLE documents_document ADD CONSTRAINT UNIQUE document_source_unique (source);

