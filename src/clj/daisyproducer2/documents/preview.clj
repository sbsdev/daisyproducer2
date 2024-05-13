(ns daisyproducer2.documents.preview
  (:require
   [babashka.fs :as fs]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.db.core :as db]
   [daisyproducer2.documents.images :as images]
   [daisyproducer2.documents.preview.dtbook2pdf :as dtbook2pdf]
   [daisyproducer2.documents.preview.dtbook2sbsform :as dtbook2sbsform]
   [daisyproducer2.documents.versions :as versions]
   [daisyproducer2.metrics :as metrics]
   [daisyproducer2.pipeline2.scripts :as scripts]
   [iapetos.collector.fn :as prometheus]))

(defn epub
  "Generate an EPUB for given `document-id` and return a tuple
  containing the name and the path of the generated epub. If no `name`
  and `target-dir` are given it will use the `version-id` as a name
  and the spool directory as a target dir."
  ([document-id]
   (let [;; if no name for the EPUB is given we use the product-id,
         ;; e.g. EB12345.epub
         product-id (or (->
                         (db/get-products {:document_id document-id :type 2}) ;; type 2 => ebook
                         :identifier)
                        "unknown") ;; FIXME
         target-dir (fs/path (env :spool-dir))]
     (epub document-id product-id target-dir)))
  ([document-id name target-dir]
   (let [dtbook (-> (versions/get-latest document-id)
                    (versions/get-content))
         images (->> (images/get-images document-id)
                     (map images/image-path))
         epub-name (str name ".epub")
         epub-path (str (fs/path target-dir epub-name))]
     (scripts/dtbook-to-ebook dtbook images epub-path)
     [epub-name epub-path])))

(defn sbsform
  "Generate an SBSForm file for given `document-id` and return a tuple
  containing the name and the path of the generated sbsform file. An
  exception is thrown if the document has no versions."
  ([document-id opts]
   (let [dtbook (-> (versions/get-latest document-id)
                    (versions/get-content))
         name (str document-id ".sbsform")
         target-dir (fs/path (env :spool-dir))
         path (str (fs/path target-dir name))]
     (dtbook2sbsform/sbsform dtbook path opts)
     [name path])))

(defn large-print
  "Generate an Large Print file for given `document-id` and return a
  tuple containing the name and the path of the generated file. An
  exception is thrown if the document has no versions."
  ([document-id opts]
   (let [dtbook (-> (versions/get-latest document-id)
                    (versions/get-content))
         images (->> (images/get-images document-id)
                     (map images/image-path))
         name (format "%s_%spt.pdf" document-id (:font-size opts))
         target-dir (fs/path (env :spool-dir))
         path (str (fs/path target-dir name))]
     (dtbook2pdf/dtbook2pdf dtbook images path opts)
     [name path])))

(defn open-document
  "Generate an OpenDocument Text Document (ODT) file for given
  `document-id` and return a tuple containing the name and the path of
  the generated file. An exception is thrown if the document has no
  versions."
  ([document-id opts]
   (let [dtbook (-> (versions/get-latest document-id)
                    (versions/get-content))
         images (->> (images/get-images document-id)
                     (map images/image-path))
         name (format "%s.odt" document-id)
         target-dir (fs/path (env :spool-dir))
         path (str (fs/path target-dir name))]
     (scripts/dtbook-to-odt dtbook images path opts)
     [name path])))

(prometheus/instrument! metrics/registry #'epub)
(prometheus/instrument! metrics/registry #'sbsform)
(prometheus/instrument! metrics/registry #'large-print)
(prometheus/instrument! metrics/registry #'open-document)

