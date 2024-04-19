(ns daisyproducer2.documents.cleanup
  "Cron job to discard no longer needed artifacts after a document has been 
  closed, such as pre-computed unknown words, old versions and
  images."
  (:require [chime.core :as chime]
            [clojure.tools.logging :as log]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]
            [mount.core :refer [defstate]]
            [daisyproducer2.documents.images :as images]
            [daisyproducer2.documents.versions :as versions])
  (:import
   (java.time LocalTime Period ZonedDateTime ZoneId)))

(defn- cleanup-unknown-words []
  (->> (db/delete-unknown-words-of-closed-documents)
       (log/debugf "Removed %s unknown words of closed documents")))

(defn- cleanup-images []
  (->> (images/delete-images-of-closed-documents)
       (log/debugf "Removed %s images of closed documents")))

(defn- cleanup-old-versions []
  (->> (versions/delete-old-versions-of-closed-documents)
       (log/debugf "Removed %s versions of closed documents")))

(defn- cleanup []
  ;; cleanup old versions
  (cleanup-old-versions)
  ;; remove images
  (cleanup-images)
  ;; clean up unknown words
  (cleanup-unknown-words))

(defstate cleanup-cron
  ;; clean up closed documents every day at 22:00
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
(prometheus/instrument! metrics/registry #'cleanup-images)
(prometheus/instrument! metrics/registry #'cleanup-old-versions)
