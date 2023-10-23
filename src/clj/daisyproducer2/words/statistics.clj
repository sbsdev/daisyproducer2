(ns daisyproducer2.words.statistics
  (:require [daisyproducer2.db.core :as db]
            [daisyproducer2.documents :as docs]
            [daisyproducer2.words.unknown :as unknown]))

(defn put-statistics
  "Persist some statistics about total and unknown words for a given
  document `document-id` to the database"
  [document-id]
  (let [xml (docs/get-latest-version)
        total (unknown/get-new-words)]
    (db/insert-unknown-words-stats {:document-id document-id :total total})))
