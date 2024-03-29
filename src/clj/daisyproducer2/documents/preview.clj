(ns daisyproducer2.documents.preview
  (:require
   [babashka.fs :as fs]
   [daisyproducer2.db.core :as db]
   [daisyproducer2.documents.versions :as versions]
   [daisyproducer2.pipeline2.scripts :as scripts]
   [daisyproducer2.metrics :as metrics]
   [iapetos.collector.fn :as prometheus]
   [daisyproducer2.documents.images :as images]))

(defn epub
  "Generate an EPUB for given `document-id` and return a tuple
  containing the name and the path of the generated epub. If no `name`
  and `target-dir` are given it will use the `version-id` as a name
  and the temp directory as a target dir."
  ([document-id]
   (let [;; if no name for the EPUB is given we use the product-id,
         ;; e.g. EB12345.epub
         product-id (or (->
                         (db/get-products {:document_id document-id :type 2}) ;; type 2 => ebook
                         :identifier)
                        "unknown") ;; FIXME
         temp-dir (fs/temp-dir)]
     (epub document-id product-id temp-dir)))
  ([document-id name target-dir]
   (let [dtbook (-> (versions/get-latest document-id)
                    (versions/get-content))
         images (->> (images/get-images document-id)
                     (map images/image-path))
         epub-name (str name ".epub")
         epub-path (str (fs/path target-dir epub-name))]
     (scripts/dtbook-to-ebook dtbook images epub-path)
     [epub-name epub-path])))

(prometheus/instrument! metrics/registry #'epub)

