(ns daisyproducer2.documents.preview.dtbook2html
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [daisyproducer2.documents.utils :refer [with-tempfile]]
   [daisyproducer2.pipeline2.scripts :as scripts]
   [sigel.xslt.core :as xslt]))

(defn- filter-braille-and-add-image-refs
  [xml target]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filterBrlContractionhints.xsl"))
              (xslt/compile-xslt (io/resource "xslt/addImageRefs.xsl"))]]
    (xslt/transform-to-file xslt (fs/file xml) (fs/file target))))

(defn dtbook2html
  [input images output]
  (with-tempfile [clean-xml {:prefix "daisyproducer-" :suffix "-clean.xml"}]
    (filter-braille-and-add-image-refs input clean-xml)
    (scripts/dtbook-to-html clean-xml images output)))