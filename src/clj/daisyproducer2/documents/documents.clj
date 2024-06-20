(ns daisyproducer2.documents.documents
  (:require [babashka.fs :as fs]
            [conman.core :as conman]
            [daisyproducer2.config :refer [env]]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.documents.versions :as versions]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.uuid :as uuid]
            [iapetos.collector.fn :as prometheus]))

(defn get-documents
  [limit offset]
  (db/get-documents {:limit limit :offset offset}))

(defn find-documents
  [limit offset search]
  (db/find-documents {:limit limit :offset offset :search search}))

(defn get-document
  [id]
  (db/get-document {:id id}))

(defn get-document-for-product-number
  [product-number]
  (db/get-document-for-product-number {:product-number product-number}))

(defn get-document-for-source-or-title-and-source-edition
  [params]
  (or (db/get-document-for-source params)
      (db/get-document-for-title-and-source-edition params)))

(defn initialize-document
  "Initialize a given `document` by adding a UUIDv7 to it."
  [document]
  (assoc document :identifier (uuid/uuid)))

(defn insert-document
  [{:keys [id] :as document}]
  (let [document-root (env :document-root)
        path (fs/path (str id))
        absolute-path (fs/absolutize (fs/path document-root path))]
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    (conman/with-transaction [db/*db*]
      ;; and store the document in the db ...
      (let [document-id (-> (db/insert-document document)
                            db/get-generated-key)
            document (assoc document :document-id document-id)]
        ;; and add an initial version
        (versions/insert-initial-version document)))))

(defn update-document-meta-data
  [document]
  (db/update-document-meta-data document))

(defn delete-document
  "Delete a document given an `id`. Return the number of rows affected."
  [id]
  (db/delete-document {:id id}))

(prometheus/instrument! metrics/registry #'get-documents)
(prometheus/instrument! metrics/registry #'get-document)
(prometheus/instrument! metrics/registry #'insert-document)
(prometheus/instrument! metrics/registry #'delete-document)
