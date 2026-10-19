(ns daisyproducer2.words.statistics
  (:require [clojure.data.csv :as csv]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus])
  (:import (java.time LocalDateTime)
           (java.time.format DateTimeFormatter)))

(defn get-statistics
  "Retrieve all statistics together with the document they belong to"
  []
  (db/get-statistics))

(def ^:private csv-columns
  [:date :document-id :title :author :source :language :grade :total :unknown])

(def ^:private date-formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss"))

(defn- format-date [statistic]
  (update statistic :date #(.format ^LocalDateTime % date-formatter)))

(defn statistics->csv
  "Convert `statistics` to CSV as Excel expects it with German or Swiss
  settings: separated by semicolons and with a byte order mark, so that
  umlauts are read as UTF-8."
  [statistics]
  (with-out-str
    (print "\uFEFF") ; byte order mark
    (csv/write-csv *out*
                   (cons (map name csv-columns)
                         (map (comp (apply juxt csv-columns) format-date) statistics))
                   :separator \; :newline :cr+lf)))

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

(prometheus/instrument! metrics/registry #'get-statistics)
(prometheus/instrument! metrics/registry #'put-statistics)
