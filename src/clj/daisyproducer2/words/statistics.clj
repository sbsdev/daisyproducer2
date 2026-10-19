(ns daisyproducer2.words.statistics
  (:require [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]))

(defn put-statistics
  "Persist the `total` number of words and the number of unknown words
  for both grades of the document with `document-id`. The db only
  records the first version of a document, as it shows how well the
  dictionaries cover a new book. The unknown words of the document have
  to be in the db already. Returns the number of insertions."
  [document-id total]
  (reduce + (for [grade [1 2]
                  :let [params {:document-id document-id :grade grade}]
                  ;; counting the unknown words is costly, so skip it if
                  ;; the db would ignore the result anyway
                  :when (nil? (db/get-statistic params))]
              (let [{unknown :total} (db/get-all-unknown-words-total params)]
                (db/insert-statistic (assoc params :total total :unknown unknown))))))

(prometheus/instrument! metrics/registry #'put-statistics)
