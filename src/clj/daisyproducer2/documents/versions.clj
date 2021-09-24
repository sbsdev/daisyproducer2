(ns daisyproducer2.documents.versions
  (:require
   [daisyproducer2.db.core :as db]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.metrics :as metrics]
   [clojure.java.io :as io]
   [babashka.fs :as fs]
   [iapetos.collector.fn :as prometheus]))

(defn get-versions
  [document-id]
  (db/get-versions {:document_id document-id}))

(defn get-version
  [id]
  (db/get-version {:id id}))

(defn get-latest
  [document-id]
  (let [document-root (env :document-root)
        version (db/get-latest-version {:document_id document-id})
        path (:content version)]
    (io/file document-root path)))

(defn insert-version
  [document-id tempfile comment user]
  (let [document-root (env :document-root)
        name (str (java.util.UUID/randomUUID) ".xml")
        path (fs/path (str document-id) "versions" name)
        absolute-path (fs/absolutize (fs/path document-root path))]
    ;; validate tempfile
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; copy the contents into the archive
    (with-open [in (io/input-stream tempfile)
                out (io/output-stream (fs/file absolute-path))]
      (io/copy in out))
    ;; and store it in the db ...
    (->
     (db/insert-version {:document_id document-id :comment comment :content (str path) :user user})
     ;; ... and return the new key
     ;; FIXME: the following most likely only works on MySQL
     first
     :generated_key)))

(defn delete-version
  [id]
  (when-let [{:keys [id content]} (not-empty (db/get-version {:id id}))]
    (db/delete-version {:id id})
    (fs/delete-if-exists (fs/path (env :document-root) content))))

(prometheus/instrument! metrics/registry #'get-versions)
(prometheus/instrument! metrics/registry #'get-version)
(prometheus/instrument! metrics/registry #'get-latest)
(prometheus/instrument! metrics/registry #'insert-version)
(prometheus/instrument! metrics/registry #'delete-version)
