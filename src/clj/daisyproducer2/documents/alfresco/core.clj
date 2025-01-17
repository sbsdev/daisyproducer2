(ns daisyproducer2.documents.alfresco.core
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [daisyproducer2.documents.alfresco.client :as alfresco]
   [daisyproducer2.documents.images :refer [insert-image]]
   [daisyproducer2.documents.update-metadata :refer [update-meta-data]]
   [daisyproducer2.documents.utils :refer [with-tempdir]]
   [daisyproducer2.documents.versions :as versions]
   [daisyproducer2.metrics :as metrics]
   [iapetos.collector.fn :as prometheus]))

(defn synchronize
  [{id :id isbn :source :as doc} uid]
  (with-tempdir [tempdir {:prefix "alfresco-"}]
    (let [images (alfresco/images-for-isbn isbn)
          content (alfresco/content-for-isbn isbn)
          tempfile (fs/file tempdir "content.xml")
          comment "Updated version due synchronization with archive"]
      ;; copy all images to the tempdir
      (doseq [{:keys [name content]} images]
        (let [image-file (fs/file tempdir name)]
          (io/copy content image-file)
          ;; and add them to the given document
          (insert-image id name image-file)))
      ;; update the metadata
      (update-meta-data content tempfile doc)
      ;; validate the tempfile
      (let [validation-errors (versions/validate-version tempfile id)]
        (if (seq validation-errors)
          {:status :invalid-dtbook :errors validation-errors}
          (do
            ;; insert an updated version
            (verions/insert-version id tempfile comment uid)
            {:status :synchronized}))))))

(defn archived? [{isbn :source}]
  (alfresco/archived? isbn))

(prometheus/instrument! metrics/registry #'synchronize)
