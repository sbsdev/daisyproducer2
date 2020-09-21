(ns dp2.documents
  (:require
   [dp2.db.core :as db]
   [dp2.config :refer [env]]
   [clojure.java.io :as io]))

(defn get-latest-version
  [document-id]
  (let [document-root (env :document-root)
        version (db/get-latest-version {:document_id document-id})
        path (:content version)]
    (io/file document-root path)))
