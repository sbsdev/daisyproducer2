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
   (db/get-images {:document-id document-id}))
  ([document-id limit offset]
   (db/get-images {:document-id document-id :limit limit :offset offset})))

(defn find-images
  [document-id limit offset search]
  (db/find-images {:document-id document-id :limit limit :offset offset :search search}))

(defn get-image
  [id]
  (db/get-image {:id id}))

(defn insert-image
  [document-id filename tempfile]
  (let [document-root (env :document-root)
        path (fs/path (str document-id) "images" filename)
        absolute-path (fs/absolutize (fs/path document-root path))]
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; copy the contents into the archive
    (fs/copy tempfile absolute-path {:replace-existing true})
    ;; and store it in the db ...
    (->
     (db/insert-image {:document-id document-id :content (str path)})
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
  (if-let [image (db/get-image {:id id})]
    (let [deletions (db/delete-image {:id id})]
      (when-not (fs/delete-if-exists (image-path image))
        ;; if an image file does not exist we simply log that fact, but do not raise an
        ;; exception
        (log/errorf "Attempting to delete non-existing image file %s" (image-path image)))
      deletions)
    0)) ;; since we could not find the image we'll return zero deletions

(defn delete-all-images
  "Delete all images for a given `document-id`. Return the number of rows affected."
  [document-id]
  ;; we need to fetch the images first to know the path to the image file, which we
  ;; will have to delete also
  (let [images (db/get-images {:document-id document-id})]
    (if (seq images)
      (do (doseq [image images]
            (when-not (fs/delete-if-exists (image-path image))
              ;; if an image file does not exist we simply log that fact, but do not raise an
              ;; exception
              (log/errorf "Attempting to delete non-existing image file %s" (image-path image))))
          (db/delete-all-images {:document-id document-id}))
      0)))

(defn delete-images-of-closed-documents
  "Delete all images of all closed documents. Return the number of rows affected."
  []
  ;; we need to fetch the images first to know the path to the image file, which we
  ;; will have to delete also
  (let [images (db/get-images-of-closed-documents {})]
    (if (seq images)
      (do (doseq [image images]
            (when-not (fs/delete-if-exists (image-path image))
              ;; if an image file does not exist we simply log that fact, but do not raise an
              ;; exception
              (log/errorf "Attempting to delete non-existing image file %s" (image-path image))))
          (db/delete-images-of-closed-documents {}))
      0)))

(prometheus/instrument! metrics/registry #'get-images)
(prometheus/instrument! metrics/registry #'get-image)
(prometheus/instrument! metrics/registry #'insert-image)
(prometheus/instrument! metrics/registry #'delete-image)
(prometheus/instrument! metrics/registry #'delete-all-images)
(prometheus/instrument! metrics/registry #'delete-images-of-closed-documents)
