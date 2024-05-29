(ns daisyproducer2.documents
  (:require [daisyproducer2.db.core :as db]))

(defn get-documents
  [limit offset]
  (db/get-documents {:limit limit :offset offset}))

(defn find-documents
  [limit offset search]
  (db/find-documents {:limit limit :offset offset :search (db/search-to-sql search)}))

(defn get-document
  [id]
  (db/get-document {:id id}))

(defn insert-document
  []
  (let [document-root (env :document-root)
        path (fs/absolutize (fs/path document-root (str document-id)))]
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; create subfolders
    (fs/copy tempfile absolute-path)
    ;; store it in the db ...
    (->
     (db/insert-version {:document_id document-id :comment comment :content (str path) :user user})
     ;; ... and return the new key
     db/get-generated-key)))

