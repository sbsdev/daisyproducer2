(ns daisyproducer2.documents.preview.dtbook2odt
  (:require
   [babashka.fs :as fs]
   [clojure.java.io :as io]
   [daisyproducer2.documents.utils :refer [with-tempfile]]
   [daisyproducer2.pipeline2.scripts :as scripts]
   [sigel.xslt.core :as xslt]))

(defn- filter-implicit-headings
  [xml target]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filter_implicit_headings.xsl"))]]
    (xslt/transform-to-file xslt (fs/file xml) (fs/file target))))

(defn dtbook2odt
  [input images output opts]
  (with-tempfile [clean-xml {:prefix "daisyproducer-" :suffix "-clean.xml"}]
    (filter-implicit-headings input clean-xml)
    (scripts/dtbook-to-odt  clean-xml images output opts)))
