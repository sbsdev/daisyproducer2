(ns daisyproducer2.documents.images
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [clojure.tools.logging :as log]
            [daisyproducer2.config :refer [env]]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]))

(defn get-images
  ([document-id]
   (db/get-images {:document_id document-id}))
  ([document-id limit offset]
   (db/get-images {:document_id document-id :limit limit :offset offset})))

(defn find-images
  [document-id limit offset search]
  (db/find-images {:document_id document-id :limit limit :offset offset :search search}))

(defn get-image
  [document-id id]
  (db/get-image {:document_id document-id :id id}))

(defn insert-image
  [document-id filename tempfile]
  (let [document-root (env :document-root)
        path (fs/path (str document-id) "images" filename)
        absolute-path (fs/absolutize (fs/path document-root path))]
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; copy the contents into the archive
    (with-open [in (io/input-stream tempfile)
                out (io/output-stream (fs/file absolute-path))]
      (io/copy in out))
    ;; and store it in the db ...
    (->
     (db/insert-image {:document_id document-id :content (str path)})
     ;; ... and return the new key
     db/get-generated-key)))

(defn image-path [image]
  (let [document-root (env :document-root)]
    (fs/path document-root (:content image))))

(defn delete-image
  "Delete an image given a `document-id` and an image `id`. Return the number of rows affected."
  [document-id id]
  ;; we need to fetch the image first to know the path to the image file, which we will have to
  ;; delete also
  (if-let [image (db/get-image {:document_id document-id :id id})]
    (let [deletions (db/delete-image {:id id})]
      (when-not (fs/delete-if-exists (image-path image))
        ;; if an image file does not exist we simply log that fact, but do not raise an
        ;; exception
        (log/errorf "Attempting to delete non-existing image file %s" (image-path image)))
      deletions)
    0)) ;; since we could not find the image we'll return zero deletions

(prometheus/instrument! metrics/registry #'get-images)
(prometheus/instrument! metrics/registry #'get-image)
(prometheus/instrument! metrics/registry #'insert-image)
(prometheus/instrument! metrics/registry #'delete-image)
