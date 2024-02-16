(ns daisyproducer2.documents.cleanup
  "Cron job to discard no longer needed artifacts after a document has been 
  finished, such as pre-computed unknown words, old versions and
  images."
  (:require [chime.core :as chime]
            [clojure.tools.logging :as log]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]
            [mount.core :refer [defstate]])
  (:import
   (java.time LocalTime Period ZonedDateTime ZoneId)))

(defn- cleanup-unknown-words []
  (->> (db/delete-unknown-words-of-finished-documents)
       (log/debugf "Removed %s unknown words of finished documents")))

(defn- cleanup []
  ;; cleanup old versions
  ;; remove images
  ;; clean up unknown words
  (cleanup-unknown-words))

(defstate cleanup-cron
  ;; clean up finished documents every day at 22:00
  :start (chime/chime-at
          (chime/periodic-seq
           (-> (LocalTime/of 22 0 0)
               (.adjustInto (ZonedDateTime/now (ZoneId/of "UTC")))
               .toInstant)
           (Period/ofDays 1))
          (fn [_] (cleanup)))
  :stop (when cleanup-cron
          (.close cleanup-cron)))

(prometheus/instrument! metrics/registry #'cleanup-unknown-words)
