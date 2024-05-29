(ns daisyproducer2.abacus.validation
  "Validate XML files from ABACUS"
  (:require [clojure.java.io :as io]
            [daisyproducer2.documents.schema-validation :refer [validation-errors]])
  (:import javax.xml.XMLConstants
           org.xml.sax.SAXException
           javax.xml.validation.SchemaFactory
           javax.xml.transform.stream.StreamSource))

(def ^:private abacus-schema "schema/abacus_export.rng")

(defn abacus-validation-errors
  "Check if an export `file` from ABACUS is a valid request. Return an
  empty list if the file is valid or a list of validation errors
  otherwise"
  [file]
  (validation-errors file abacus-schema))

