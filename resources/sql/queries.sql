---------------
-- Documents --
---------------

-- :name get-documents :? :*
-- :doc retrieve all documents given a limit and an offset
SELECT doc.*, (CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END) AS spelling, state.name AS state
FROM documents_document doc
JOIN documents_state state
ON doc.state_id = state.id
ORDER BY doc.state_id
LIMIT :limit OFFSET :offset

-- :name find-documents :? :*
-- :doc retrieve all documents given a `search` term, a `limit` and an `offset`
SELECT doc.*, (CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END) AS spelling, state.name AS state
FROM documents_document doc
JOIN documents_state state
ON doc.state_id = state.id
WHERE LOWER(doc.title) LIKE LOWER(:search) OR LOWER(doc.author) LIKE LOWER(:search)
ORDER BY doc.state_id
LIMIT :limit OFFSET :offset

-- :name get-document :? :1
-- :doc retrieve a document record given the `id`
SELECT doc.*, (CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END) AS spelling, state.name AS state
FROM documents_document doc
JOIN documents_state state
ON doc.state_id = state.id
WHERE doc.id = :id

-- :name insert-document :insert :raw
-- :doc Insert a new document with given `title`, `author`, `publisher`, `date`, `identifier`, `language` and optionally `subject`, `description`, `source`, `rights`, `source-date`, `source-edition`, `source-publisher`, `source-rights`, `production-series`, `production-series-number` and `production-source` .
INSERT INTO documents_document (
--~ (when (:subject params) "subject,")
--~ (when (:description params) "description,")
--~ (when (:source params) "source,")
--~ (when (:rights params) "rights,")
--~ (when (:source-date params) "source_date,")
--~ (when (:source-edition params) "source_edition,")
--~ (when (:source-publisher params) "source_publisher,")
--~ (when (:source-rights params) "source_rights,")
--~ (when (:production-series params) "production_series,")
--~ (when (:production-series-number params) "production_series_number,")
--~ (when (:production-source params) "production_source,")
       title, author, publisher, date, identifier, language)
VALUES (
--~ (when (:subject params) ":subject,")
--~ (when (:description params) ":description,")
--~ (when (:source params) ":source,")
--~ (when (:rights params) ":rights,")
--~ (when (:source-date params) ":source-date,")
--~ (when (:source-edition params) ":source-edition,")
--~ (when (:source-publisher params) ":source-publisher,")
--~ (when (:source-rights params) ":source-rights,")
--~ (when (:production-series params) ":production-series,")
--~ (when (:production-series-number params) ":production-series-number,")
--~ (when (:production-source params) ":production-source,")
	:title, :author, :publisher, :date, :identifier, :language)

-- :name update-document-state :! :n
-- :doc update the `state` field of a document with given `id`. The state can be either "open" or "closed"
UPDATE documents_document
SET state_id = (CASE :state WHEN "open" THEN 7 WHEN "closed" THEN 8 END)
WHERE id = :id

-- :name update-document-meta-data :! :n
-- :doc update the meta data of a document with given `id`. Note: `publisher` and `language` cannot be updated.
UPDATE documents_document
SET title = :title,
    author = :author,
    date = :date,
    description = :description,
    rights = :rights,
    source = :source,
    subject = :subject,
    source_date = :source-date,
    source_publisher = :source-publisher,
    source_edition = :source-edition,
    source_rights = :source-rights,
    production_series = :production-series,
    production_series_number = :production-series-number,
    production_source = :production-source
WHERE id = :id

-- :name delete-document :! :n
-- :doc Delete a document given the `id`.
DELETE FROM documents_document WHERE id = :id

-- :name get-document-for-product-number :? :1
-- :doc retrieve the document for the given `product-number`
SELECT doc.* FROM documents_document doc
JOIN documents_product prod
ON doc.id = prod.document_id
WHERE prod.identifier = :product-number

-- :name get-document-for-source :? :1
-- :doc retrieve the document for the given `source`
SELECT * FROM documents_document
WHERE source = :source

-- :name get-document-for-title-and-source-edition :? :1
-- :doc retrieve the document for the given `title` and `source-edition`
SELECT * FROM documents_document
WHERE title = :title AND source_edition = :source-edition

--------------
-- Products --
--------------

-- :name get-products :? :*
-- :doc retrieve all product record given the associated `document-id` and an optional `type`
SELECT * FROM documents_product
WHERE document_id = :document-id
--~ (when (:type params) "AND type = :type")

-- :name get-product :? :1
-- :doc retrieve a product for given `id`
SELECT * FROM documents_product
WHERE id = :id

-- :name insert-product :insert :raw
-- :doc Insert a new product for a given `document-id` with given `product-number` and `type`.
INSERT INTO documents_product (identifier, type, document_id)
VALUES (:product-number, :type, :document-id)

-- :name delete-product :! :n
-- :doc Delete a version.
DELETE FROM documents_product WHERE id = :id

--------------
-- Versions --
--------------

-- :name get-versions :? :*
-- :doc retrieve all versions of a document given a `document-id`
SELECT * FROM documents_version
WHERE document_id = :document-id
ORDER BY created_at DESC
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name find-versions :? :*
-- :doc retrieve all versions given a `document-id`, a `search` term, a `limit` and an `offset`
SELECT * FROM documents_version
WHERE document_id = :document-id
AND LOWER(comment) LIKE LOWER(:search)
ORDER BY created_at DESC
LIMIT :limit OFFSET :offset

-- :name get-version :? :1
-- :doc retrieve a version for given `id`
SELECT * FROM documents_version
WHERE id = :id

-- :name get-latest-version :? :1
-- :doc retrieve the latest version of a document given a `document-id`
SELECT * FROM documents_version
WHERE document_id = :document-id
AND created_at = (SELECT MAX(created_at) FROM documents_version WHERE document_id = :document-id)

-- :name insert-version :insert :raw
-- :doc Insert a new version for a given `document-id` with given `comment`, `content` and `user`.
INSERT INTO documents_version (comment, document_id, content, created_by)
VALUES (:comment, :document-id, :content, :user)

-- :name delete-version :! :n
-- :doc Delete a version.
DELETE FROM documents_version WHERE id = :id

-- :name get-old-versions-of-closed-documents :? :*
-- :doc Get old versions that belong to any document that is in "closed" state
SELECT * FROM documents_version version
JOIN documents_document doc
ON version.document_id = doc.id
JOIN documents_state state
ON doc.state_id = state.id
WHERE state.name = "closed"
AND version.id NOT IN (
    SELECT MAX(id) FROM documents_version
    GROUP BY document_id)

-- :name delete-old-versions-of-closed-documents :! :n
-- :doc Remove old versions that belong to any document that is in "closed" state
DELETE version
FROM documents_version version
JOIN documents_document doc
ON version.document_id = doc.id
JOIN documents_state state
ON doc.state_id = state.id
WHERE state.name = "closed"
AND version.id NOT IN (
    -- the nested select is needed to avoid a mysql error, see https://stackoverflow.com/a/43171707
    SELECT * FROM (
    SELECT MAX(id) FROM documents_version
    GROUP BY document_id) t)

------------
-- Images --
------------

-- :name get-images :? :*
-- :doc retrieve all images of a document given a `document-id`. Optionally the results can be limited by `limit` and `offset`
SELECT * FROM documents_image
WHERE document_id = :document-id
ORDER BY content
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name find-images :? :*
-- :doc retrieve all images given a `document-id`, a `search` term, a `limit` and an `offset`
SELECT * FROM documents_image
WHERE document_id = :document-id
AND LOWER(content) LIKE LOWER(:search)
ORDER BY content
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name get-image :? :1
-- :doc retrieve an image for the given `id`
SELECT * FROM documents_image
WHERE id = :id

-- :name insert-image :insert :raw
-- :doc Insert a new image for a given `document-id` with given and `content`.
INSERT INTO documents_image (document_id, content)
VALUES (:document-id, :content)
ON DUPLICATE KEY UPDATE
content = VALUES(content),
document_id = VALUES(document_id)

-- :name delete-image :! :n
-- :doc Delete an image.
DELETE FROM documents_image WHERE id = :id

-- :name delete-all-images :! :n
-- :doc Delete all image for a for a given `document-id`.
DELETE FROM documents_image WHERE document_id = :document-id

-- :name get-images-of-closed-documents :? :*
-- :doc Get all images that belong to any document that is in "closed" state
SELECT * FROM documents_image image
JOIN documents_document doc
ON image.document_id = doc.id
JOIN documents_state state
ON doc.state_id = state.id
WHERE state.name = "closed"

-- :name delete-images-of-closed-documents :! :n
-- :doc Delete all images that belong to any document that is in "closed" state
DELETE image
FROM documents_image image
JOIN documents_document doc
ON image.document_id = doc.id
JOIN documents_state state
ON doc.state_id = state.id
WHERE state.name = "closed"

------------------
-- Global Words --
------------------

-- :name get-global-words :? :*
-- :doc retrieve all global words optionally filtered by `type` and optionally limited by `limit` and `offset`
SELECT untranslated, uncontracted, contracted, type, homograph_disambiguation
FROM dictionary_globalword
--~ (when (:type params) "WHERE type = :type")
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name find-global-words :? :*
-- :doc retrieve all global words given a `search` term, a `limit` and an `offset` and optionally filtered by `type`
SELECT untranslated, uncontracted, contracted, type, homograph_disambiguation
FROM dictionary_globalword
WHERE untranslated LIKE :search
--~ (when (:type params) "AND type = :type")
ORDER BY untranslated
LIMIT :limit OFFSET :offset

-- :name get-global-words-with-braille :? :*
-- :doc retrieve all global words where the column `braille` ("contracted" or "uncontracted") is not null optionally filtered by `types`
SELECT untranslated, uncontracted, contracted, type, homograph_disambiguation
FROM dictionary_globalword
WHERE :i:braille IS NOT NULL
--~ (when (:types params) "AND type IN (:v*:types)")

-- :name insert-global-word :! :n
-- :doc Insert or update a word in the global dictionary.
INSERT INTO dictionary_globalword (untranslated, uncontracted, contracted, type, homograph_disambiguation)
VALUES (:untranslated, :uncontracted, :contracted, :type, :homograph-disambiguation)
ON DUPLICATE KEY UPDATE
contracted = VALUES(contracted),
uncontracted = VALUES(uncontracted)

-- :name delete-global-word :! :n
-- :doc Delete a word in the global dictionary.
DELETE FROM dictionary_globalword
WHERE untranslated = :untranslated
AND type = :type
AND homograph_disambiguation = :homograph-disambiguation

-----------------
-- Local Words --
-----------------

-- :name get-local-words :? :*
-- :doc retrieve all local words for a given document `id` and `grade`. Optionally you can only get local words that match a `search` term. The words contain braille for both grades and the hyphenation if they exist. Optionally the results can be limited by `limit` and `offset`.
SELECT words.untranslated,
--~ (when (#{0 1} (:grade params)) "words.uncontracted,")
--~ (when (#{0 2} (:grade params)) "words.contracted,")
       words.type,
       words.homograph_disambiguation,
       words.document_id,
       words.isLocal,
       (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END FROM documents_document WHERE id = :id) AS spelling,
       hyphenation.hyphenation AS hyphenated
FROM dictionary_localword as words
LEFT JOIN hyphenation_words AS hyphenation
ON words.untranslated = hyphenation.word
AND hyphenation.spelling =
  (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
  FROM  documents_document
  WHERE id = :id)
WHERE words.document_id = :id
-- either uncontracted or contracted should always be non-null so no
-- need to query for either being non-null in the case of grade 0
--~ (when (= (:grade params) 1) "AND words.uncontracted IS NOT NULL")
--~ (when (= (:grade params) 2) "AND words.contracted IS NOT NULL")
--~ (when (:search params) "AND words.untranslated LIKE :search")
ORDER BY words.untranslated
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name get-local-word :? :1
-- :doc retrieve a local word record given `document-id`, `untranslated`, `type` and `homograph-disambiguation`.
SELECT *
FROM dictionary_localword
WHERE untranslated = :untranslated
AND type = :type
AND homograph_disambiguation = :homograph-disambiguation
AND document_id = :document-id

-- :name insert-local-word :! :n
-- :doc Insert or update a word in the local dictionary. Optionally specify `isconfirmed`.
INSERT INTO dictionary_localword (
       untranslated,
--~ (when (:contracted params) "contracted,")
--~ (when (:uncontracted params) "uncontracted,")
       type,
       homograph_disambiguation,
       document_id,
       isLocal,
       isConfirmed)
VALUES (
       :untranslated,
--~ (when (:contracted params) ":contracted,")
--~ (when (:uncontracted params) ":uncontracted,")
       :type,
       :homograph-disambiguation,
       :document-id,
       :islocal,
--~ (if (:isconfirmed params) ":isconfirmed" "DEFAULT")
       )
ON DUPLICATE KEY UPDATE
--~ (when (:contracted params) "contracted = VALUES(contracted),")
--~ (when (:uncontracted params) "uncontracted = VALUES(uncontracted),")
--~ (when (:isconfirmed params) "isConfirmed = VALUES(isConfirmed),")
isLocal = VALUES(isLocal)

-- :name delete-local-word-partial :! :n
-- :doc Set either contracted or uncontracted to NULL for given `document-id`, `untranslated`, `type` and `homograph-disambiguation`.
UPDATE dictionary_localword
/*~ (if (:contracted params) */
SET contracted = NULL
/*~*/
SET uncontracted = NULL
/*~ ) ~*/
WHERE untranslated = :untranslated
AND type = :type
AND homograph_disambiguation = :homograph-disambiguation
AND document_id = :document-id

-- :name delete-local-word :! :n
-- :doc Delete a word in the local dictionary.
DELETE FROM dictionary_localword
WHERE untranslated = :untranslated
AND type = :type
AND homograph_disambiguation = :homograph-disambiguation
AND document_id = :document-id

-------------------
-- Unknown words --
-------------------

-- :name delete-unknown-words :! :n
-- :doc empty the "temporary" table containing words from a new document for given `:document-id`
DELETE FROM dictionary_unknownword
WHERE document_id = :document-id

-- :name insert-unknown-words :! :n
-- :doc insert a list of new `words` into a "temporary" table. This later used to join with the already known words to query the unknown words
INSERT INTO dictionary_unknownword (untranslated, type, homograph_disambiguation, document_id)
VALUES :tuple*:words

-- :name update-unknown-word :! :n
-- :doc update the `isIgnored` field of an unknown `word`
UPDATE dictionary_unknownword
SET isIgnored = :isignored, isLocal = :islocal
WHERE untranslated = :untranslated
AND type = :type
AND homograph_disambiguation = :homograph-disambiguation
AND document_id = :document-id

-- :name delete-non-existing-unknown-words-from-local-words :! :n
-- :doc delete words that are not in the list of unknown words from the local words for given `:document-id`
DELETE l
FROM dictionary_localword l
LEFT JOIN dictionary_unknownword u
ON u.untranslated = l.untranslated AND u.type = l.type AND u.document_id = l.document_id
WHERE u.untranslated IS NULL
AND l.document_id = :document-id

-- :name delete-unknown-words-of-closed-documents :! :n
-- :doc Remove all unknown words that belong to any document that is in "closed" state
DELETE unknown
FROM dictionary_unknownword unknown
JOIN documents_document doc
ON unknown.document_id = doc.id
JOIN documents_state state
ON doc.state_id = state.id
WHERE state.name = "closed"

-- :name get-all-unknown-words :? :*
-- :doc given a `document-id` and a `:grade` retrieve all unknown words for it. If `:grade` is 0 then return words for both grade 1 and 2. Otherwise just return the unknown words for the given grade.This assumes that the new words contained in this document have been inserted into the `dictionary_unknownword` table.
-- NOTE: This query assumes that there are only records for the current document-id in the dictionary_unknownword table.
(SELECT unknown.*,
--~ (when (#{0 1} (:grade params)) "COALESCE(l.uncontracted, g.uncontracted) AS uncontracted,")
--~ (when (#{0 2} (:grade params)) "COALESCE(l.contracted, g.contracted) AS contracted,")
       hyphenation.hyphenation AS hyphenated,
       (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END FROM documents_document WHERE id = :document-id) AS spelling
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (0,1,3) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (0,1,3)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 0
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
UNION
(SELECT unknown.*,
--~ (when (#{0 1} (:grade params)) "COALESCE(l.uncontracted, g.uncontracted) AS uncontracted,")
--~ (when (#{0 2} (:grade params)) "COALESCE(l.contracted, g.contracted) AS contracted,")
       hyphenation.hyphenation AS hyphenated,
       (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END FROM documents_document WHERE id = :document-id) AS spelling
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (1,2) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (1,2)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 2
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
UNION
(SELECT unknown.*,
--~ (when (#{0 1} (:grade params)) "COALESCE(l.uncontracted, g.uncontracted) AS uncontracted,")
--~ (when (#{0 2} (:grade params)) "COALESCE(l.contracted, g.contracted) AS contracted,")
       hyphenation.hyphenation AS hyphenated,
       (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END FROM documents_document WHERE id = :document-id) AS spelling
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (3,4) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (3,4)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 4
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
UNION
(SELECT unknown.*,
--~ (when (#{0 1} (:grade params)) "COALESCE(l.uncontracted, g.uncontracted) AS uncontracted,")
--~ (when (#{0 2} (:grade params)) "COALESCE(l.contracted, g.contracted) AS contracted,")
       hyphenation.hyphenation AS hyphenated,
       (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END FROM documents_document WHERE id = :document-id) AS spelling
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (5) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (5)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 5
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
ORDER BY isIgnored, untranslated
LIMIT :limit OFFSET :offset

-- :name get-all-unknown-words-total :? :1
-- :doc given a `document-id` and a `:grade` retrieve the total of all unknown words for it. If `:grade` is 0 then return words for both grade 1 and 2. Otherwise just return the unknown words for the given grade. This assumes that the new words contained in this document have been inserted into the `dictionary_unknownword` table.
SELECT
CAST(SUM(
(SELECT COUNT(*)
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (0,1,3) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (0,1,3)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 0
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
+
(SELECT COUNT(*)
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (1,2) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (1,2)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 2
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
+
(SELECT COUNT(*)
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (3,4) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (3,4)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 4
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)
+
(SELECT COUNT(*)
FROM dictionary_unknownword unknown
LEFT JOIN dictionary_localword l ON l.untranslated = unknown.untranslated AND l.type IN (5) AND l.document_id = :document-id
LEFT JOIN dictionary_globalword g ON g.untranslated = unknown.untranslated AND g.type IN (5)
LEFT JOIN hyphenation_words AS hyphenation
     ON unknown.untranslated = hyphenation.word
     AND hyphenation.spelling =
     	 (SELECT CASE language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END
	  FROM  documents_document
	  WHERE id = :document-id)
WHERE unknown.type = 5
AND unknown.document_id = :document-id
AND g.untranslated IS NULL
AND (((:grade IN (0,2)) AND l.contracted IS NULL) OR ((:grade IN (0,1)) AND l.uncontracted IS NULL))
)) AS INT) AS total

-----------------------
-- Confirmable words --
-----------------------

-- :name get-confirmable-words :? :*
-- :doc retrieve local words that are ready for confirmation. The words contain braille for both grades and the hyphenation if they exist.
SELECT words.*,
       (CASE doc.language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END) AS spelling,
       doc.title AS document_title,
       hyphenation.hyphenation AS hyphenated
FROM dictionary_localword as words
JOIN documents_document doc ON words.document_id = doc.id
LEFT JOIN hyphenation_words AS hyphenation
ON words.untranslated = hyphenation.word
AND hyphenation.spelling = (CASE doc.language WHEN "de" THEN 1 WHEN "de-1901" THEN 0 ELSE NULL END)
WHERE words.isConfirmed = FALSE
-- only get words from closed productions
AND doc.state_id = (SELECT id FROM documents_state WHERE sort_order = (SELECT MAX(sort_order) FROM documents_state))
ORDER BY words.document_id, words.untranslated
LIMIT :limit OFFSET :offset

------------------
-- Hyphenations --
------------------

-- :name get-hyphenation :? :*
-- :doc retrieve hyphenations given a `spelling` and optionally s `search` term, a `limit` and an `offset`
SELECT * FROM hyphenation_words
WHERE spelling = :spelling
--~ (when (:search params) "AND word LIKE :search")
--~ (when (:limit params) "LIMIT :limit")
--~ (when (:offset params) "OFFSET :offset")

-- :name insert-hyphenation :! :n
-- :doc Insert or update a hyphenation.
INSERT INTO hyphenation_words (word, hyphenation, spelling)
VALUES (:word, :hyphenation, :spelling)
ON DUPLICATE KEY UPDATE
hyphenation = VALUES(hyphenation)

-- :name delete-hyphenation :! :n
-- :doc Delete a hyphenation word `:word` and `:spelling` if there are no more references to it from either the local words, if `:document-id` is given, or the global words otherwise.
DELETE FROM hyphenation_words
WHERE word = :word
AND spelling = :spelling
/*~ (if (:document-id params) */
AND NOT EXISTS (
    SELECT * FROM dictionary_localword
    WHERE untranslated = :word
    AND document_id = :document-id
)
/*~*/
AND NOT EXISTS (
    SELECT * FROM dictionary_globalword
    WHERE untranslated = :word
)
/*~ ) ~*/
