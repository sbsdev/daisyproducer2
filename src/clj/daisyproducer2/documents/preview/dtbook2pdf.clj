(ns daisyproducer2.documents.preview.dtbook2pdf
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [daisyproducer2.documents.utils :refer [with-tempfile]]
   [daisyproducer2.pipeline1 :as pipeline1]
   [daisyproducer2.pipeline2.scripts :as pipeline2]
   [sigel.xslt.core :as xslt]))

(defn- prepare-xml
  "Given an `xml` filter the braille contraction hints, drop the
  implicit headings, add image references, move the list of contents
  to the end of the front matter and add the imprint. Store the
  updated xml in `target`.

  The last two were done by the conversion itself as long as it was
  the SBS variant of the dtbook2latex stylesheet, which was copied
  over the common one when the Pipeline 1 was installed. The
  conversion in the Pipeline 2 has no such hook and no SBS specific
  code, so the document says what it needs before it is converted."
  [xml target]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filterBrlContractionhints.xsl"))
              (xslt/compile-xslt (io/resource "xslt/filter_implicit_headings.xsl"))
              (xslt/compile-xslt (io/resource "xslt/addImageRefs.xsl"))
              (xslt/compile-xslt (io/resource "xslt/moveTocToEnd.xsl"))
              (xslt/compile-xslt (io/resource "xslt/addImprint.xsl"))]]
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

(def ^:private words-per-volume
  "The words that go in a volume, by font size, i.e. the words that fit
  on the roughly 200 pages that are still comfortable to bind. A page
  holds 134 words at 17pt, measured over the large print books of a
  production year, and 0.77 respectively 0.54 of that at 20pt and
  25pt. A book is split into as many equally thick volumes as these
  words ask for, so the figure only has to be right on average."
  {17 26000 20 20000 25 14000})

(defn- generate-pdf
  [input images output opts]
  (with-tempfile [latex-file {:prefix "daisyproducer-" :suffix ".tex"}]
    (pipeline2/dtbook-to-latex input latex-file opts)
    (pipeline1/latex-to-pdf latex-file images output)))

(def ^:private large-print-defaults
  {:font-size 17 :font :tiresias :page-style :plain :alignment :left
   ;; the words and phrases that the conversion generates itself are in
   ;; Swiss German, whatever the language of the book is
   :producer-language "de-CH"
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
        opts (maybe-set-compact-style opts input)
        opts (assoc opts :words-per-volume (words-per-volume (:font-size opts)))]
    (with-tempfile [clean-xml {:prefix "daisyproducer-" :suffix "-clean.xml"}]
      (prepare-xml input clean-xml)
      (generate-pdf clean-xml images output opts))))
