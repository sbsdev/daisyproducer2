(ns daisyproducer2.documents.preview.dtbook2pdf
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [clojure.math :as math]
   [daisyproducer2.documents.utils :refer [with-tempfile]]
   [daisyproducer2.pipeline1 :as pipeline1]
   [sigel.xslt.core :as xslt])
  (:import
   (org.apache.pdfbox Loader)))

(defn- prepare-xml
  "Given an `xml` filter the braille contraction hints, drop the
  implicit headings and add image references. Store the updated xml in
  `target`."
  [xml target]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filterBrlContractionhints.xsl"))
              (xslt/compile-xslt (io/resource "xslt/filter_implicit_headings.xsl"))
              (xslt/compile-xslt (io/resource "xslt/addImageRefs.xsl"))]]
    (xslt/transform-to-file xslt (fs/file xml) (fs/file target))))

(defn- compact?
  "Given an `xml` determine whether it should be rendered using
  compactStyle. Return true if it only contains `level1`. Return true
  if it contains `level2` but all `h2` are empty. Return false
  otherwise."
  [xml]
  (let [xslt (xslt/compile-xslt (io/resource "xslt/isCompactStyle.xsl"))]
    (= "true" (str (xslt/transform xslt (fs/file xml))))))

(defn- maybe-set-compact-style
  "Change the `page-style` option to `:compact` is it is currently
  `:plain` and the `xml` is `compact?`"
  [{:keys [page-style] :as opts} xml]
  (cond-> opts
    (and (= page-style :plain) (compact? xml)) (assoc :page-style :compact)))

(defn- generate-pdf
  [input images output opts]
  (with-tempfile [latex-file {:prefix "daisyproducer-" :suffix ".tex"}]
    (pipeline1/dtbook-to-latex input latex-file opts)
    (pipeline1/latex-to-pdf latex-file images output)))

(defn- get-number-of-volumes
  [pdf]
  (let [document (Loader/loadPDF (io/file pdf))
        pages (.getNumberOfPages document)
        pages-per-volume 200
        volumes (int (math/ceil (/ pages pages-per-volume)))]
    (.close document)
    volumes))

(def ^:private large-print-defaults
  {:font-size 17 :font :tiresias :page-style :plain :alignment :left
   :stock-size :a4paper :line-spacing :onehalfspacing
   :paperwidth 200 :paperheight 250
   :left-margin 28 :right-margin 20 :top-margin 20 :bottom-margin 20
   :replace-em-with-quote true :end-notes :none :image-visibility :ignore
   :backup-font "Arial" :backup-unicode-ranges "Arabic,Hebrew,Cyrillic,GreekAndCoptic"})

(defn dtbook2pdf
  [input images output opts]
  (let [opts (merge large-print-defaults opts)
        ;; when the page style is not explicitely requested check
        ;; whether the book should be rendered using compact style
        opts (maybe-set-compact-style opts input)]
    (with-tempfile [clean-xml {:prefix "daisyproducer-" :suffix "-clean.xml"}]
      (with-tempfile [pdf-no-volumes {:prefix "daisyproducer-" :suffix "-no-volumes.pdf"}]
        (with-tempfile [split-xml {:prefix "daisyproducer-" :suffix "-split.xml"}]
          (prepare-xml input clean-xml)
          (generate-pdf clean-xml images pdf-no-volumes opts)
          (pipeline1/insert-volume-split-points clean-xml split-xml (get-number-of-volumes (fs/file pdf-no-volumes)))
          (generate-pdf split-xml images output opts))))))
