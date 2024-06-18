(ns daisyproducer2.documents.documents
  (:require [daisyproducer2.db.core :as db]
            [daisyproducer2.config :refer [env]]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]
            [babashka.fs :as fs]))

(defn get-documents
  [limit offset]
  (db/get-documents {:limit limit :offset offset}))

(defn find-documents
  [limit offset search]
  (db/find-documents {:limit limit :offset offset :search search}))

(defn get-document
  [id]
  (db/get-document {:id id}))

(defn insert-document
  [{:keys [id] :as document}]
  (let [document-root (env :document-root)
        path (fs/path (str id))
        absolute-path (fs/absolutize (fs/path document-root path))]
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; add an initial version
    ;; and store it in the db ...
    (->
     (db/insert-document document)
     ;; ... and return the new key
     db/get-generated-key)))

(defn delete-document
  "Delete a document given an `id`. Return the number of rows affected."
  [id]
  (db/delete-document {:id id}))

(prometheus/instrument! metrics/registry #'get-documents)
(prometheus/instrument! metrics/registry #'get-document)
(prometheus/instrument! metrics/registry #'insert-document)
(prometheus/instrument! metrics/registry #'delete-document)