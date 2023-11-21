(ns daisyproducer2.words.local
  (:require [clojure.string :as string]
            [clojure.tools.logging :as log]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.whitelists.async :as whitelists]
            [daisyproducer2.whitelists.hyphenation :as hyphenations]
            [daisyproducer2.words :as words]
            [iapetos.collector.fn :as prometheus]))

(defn- remove-empty-vals
  [{:keys [contracted uncontracted] :as word}]
  (cond-> word
    (nil? contracted) (dissoc :contracted)
    (nil? uncontracted) (dissoc :uncontracted)))

(defn get-words
  "Retrieve all local words for given document-id `id`, `grade` and
  a (possibly nil) `search` term. Limit the result set by `limit` and
  `offset`."
  [id grade search limit offset]
  (let [document (db/get-document {:id id})
        spelling (:spelling document)
        params (cond-> {:id id :limit limit :offset offset :grade grade}
                 (not (string/blank? search)) (assoc :search (db/search-to-sql search)))
        words (db/get-local-words params)]
    (->> words
         (map words/int-fields-to-boolean)
         ;; there are local words where we have only either contracted
         ;; or uncontracted. Despite all my efforts the db will return
         ;; a NULL value for those fields in that case.
         (map remove-empty-vals)
         (map words/complement-hyphenation))))

(defn put-word
  "Persist a `word` in the db. Upsert all braille translations and the
  hyphenation. Returns the number of insertions/updates."
  [word]
  (log/debug "Add local word" word)
  (when (:hyphenated word)
    (db/insert-hyphenation
     (words/to-db word words/hyphenation-keys words/hyphenation-mapping))
    (hyphenations/export))
  (let [insertions
        (db/insert-local-word
         (words/to-db word words/dictionary-keys words/dictionary-mapping))]
    (whitelists/export-local-tables (:document-id word))
    insertions))

(defn- delete-word-and-hyphenation
  "Uncoditionally delete `word` and its associated hyphenation. Returns
  the number of deleted words, i.e. 0 or 1."
  [{:keys [hyphenated document-id] :as word}]
  (let [deletions
        (db/delete-local-word
         (words/to-db word words/dictionary-keys words/dictionary-mapping))]
    (when (and hyphenated (> deletions 0))
      (db/delete-hyphenation
       (words/to-db word words/hyphenation-keys words/hyphenation-mapping))
      (hyphenations/export))
    (whitelists/export-local-tables document-id)
    deletions))

(defn delete-word
  "Remove a `word` from the db. If the word contains both
  `:uncontracted` and `:contracted` then delete the db record. If the
  word only contains either `:uncontracted` or `:contracted` then
  update the db record and set the column to NULL. In the case the
  other column was already NULL delete the whole db record. On
  deletion also remove it from the hyphenations table. Returns the
  number of deletions."
  [{:keys [contracted uncontracted hyphenated document-id] :as word}]
  (log/debug "Delete local word" word)
  (if (and contracted uncontracted)
    ;; delete the word and the associated hyphenation
    (delete-word-and-hyphenation word)
    ;; update the word or maybe delete it if the update would result
    ;; in both contracted and uncontracted becoming NULL
    (let [{old-contracted :contracted old-uncontracted :uncontracted}
          (db/get-local-word
           (words/to-db word words/dictionary-keys words/dictionary-mapping))]
      (if (or (and contracted (nil? old-uncontracted))
              (and uncontracted (nil? old-contracted)))
        (delete-word-and-hyphenation word)
        (db/delete-local-word-partial
         (words/to-db word words/dictionary-keys words/dictionary-mapping))))))

(prometheus/instrument! metrics/registry #'get-words)
(prometheus/instrument! metrics/registry #'put-word)
(prometheus/instrument! metrics/registry #'delete-word)
