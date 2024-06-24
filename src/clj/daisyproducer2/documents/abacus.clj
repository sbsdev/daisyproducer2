(ns daisyproducer2.documents.abacus
  "Import files from ABACUS

  The interface to our ERP is crude: you basically read files from an
  directory. The import from ABACUS is done via XML. The interface
  imports the following notifications:

  - Import a document."
  (:require [clojure.data.zip.xml :refer [text xml-> xml1->]]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [daisyproducer2.documents.documents :as documents]
            [daisyproducer2.documents.products :as products]
            [daisyproducer2.documents.schema-validation :refer [validation-errors]]
            [daisyproducer2.documents.versions :as versions]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]
            [clojure.string :as string]))

(s/def ::product-number (s/and string? #(re-matches #"^(PS|GD|EB|ET)\d{4,7}$" %)))

(def ^:private root-path [:Task :Transaction :DocumentData])

(def ^:private param-mapping
  {:product-number [:artikel_nr text]
   :title [:MetaData :dc :title text]
   :author [:MetaData :dc :creator text]
   :date [:MetaData :dc :date text]
   :source [:MetaData :dc :source text]
   :language [:MetaData :dc :language text]
   :source-publisher [:MetaData :sbs :verlag text]
   :source-edition [:MetaData :sbs :auflageJahr text]
   :production-series-number [:MetaData :sbs :rucksackNr text]
   :reihe [:MetaData :sbs :reihe text]
   :aufwand [:MetaData :sbs :Aufwand_A2 text]
   :verkaufstext [:MetaData :sbs :verkaufstext text]
   :daisyproducer? [:MetaData :sbs :daisy_producer text]
   })

(defn- production-series
  [{:keys [production-series-number reihe]}]
  (cond
    (not= production-series-number "0") "PPP"
    (and (= production-series-number "0")
         (not (str/blank? reihe))
         (str/includes? reihe "SJW")) "SJW"))

(defn- production-series-number
  [{:keys [production-series-number reihe]}]
  (cond
    (not= production-series-number "0") production-series-number
    (and (= production-series-number "0")
         (not (str/blank? reihe))
         (str/includes? reihe "SJW")) (re-find #"\d+" reihe)))

(defn- product-type
  [{:keys [product-number]}]
  (condp #(str/starts-with? %2 %1) product-number
    "PS" :braille
    "GD" :large-print
    "EB" :ebook
    "ET" :etext
    nil))

(def ^:private default-publisher {"de" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                  "it" "Unitas - Associazione ciechi e ipovedenti della Svizzera italiana"})

(defn clean-raw-document
  "Return a proper document based on a raw document"
  [{:keys [language source aufwand verkaufstext daisyproducer?] :as raw-document}]
  (let [production-series (production-series raw-document)
        production-series-number (production-series-number raw-document)
        product-type (product-type raw-document)
    (-> raw-document
        (dissoc :reihe :aufwand :verkaufstext)
        (assoc :publisher (get default-publisher language "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"))
        (assoc :daisyproducer? (= daisyproducer? "ja"))
        (cond-> (or (str/blank? source) (= source "keine")) (dissoc :source))
        (cond-> (= aufwand "D") (assoc :production-source "electronicData"))
        (cond-> product-type (assoc :product-type product-type))
        (cond-> (not (str/blank? verkaufstext)) (assoc :author (-> verkaufstext (str/split #"\[xx\]") first str/trim)))
        (cond-> (not (str/blank? verkaufstext)) (assoc :title (-> verkaufstext (str/split #"\[xx\]") second str/trim))))))

(defn extract-value
  "Extract values from a `zipper` from an ABACUS export file for `key`"
  [zipper key]
  (let [path (key param-mapping)]
    (case key
      :narrator (string/join "; " (apply xml-> zipper (concat root-path path)))
      (apply xml1-> zipper (concat root-path path)))))

(defn read-file
  "Read an export file from ABACUS and return a map with all the data,
  i.e. a document"
  [file]
  (let [zipper (-> file io/file xml/parse zip/xml-zip)]
    (->>
     (for [key (keys param-mapping)
           :let [val (extract-value zipper key)]
           :when (some? val)]
       [key val])
      (into {})
      clean-raw-document)))

(def ^:private abacus-export-schema "schema/abacus_export.rng")

(defn- metadata-changed?
  [old new]
  (let [relevant-keys #{:title :author :date :source
                        :source-date :source-publisher :source-edition
                        :production-series :production-series-number :production-source}]
    (not= (select-keys old relevant-keys) (select-keys new relevant-keys))))

(defn- update-document
  [old new]
  (when (metadata-changed? old new)
    (documents/update-document-meta-data new)
    #_(versions/insert-version)))

(defn import-new-document
  "Import a new document from file `f`"
  [f]
  (let [errors (validation-errors f abacus-export-schema)]
    (if (empty? errors)
      (let [{:keys [product-number title] :as document} (read-file f)]
        (cond
          ;; If the XML indicates that this product is not produced with Daisy Producer ignore this file
          (not (:daisyproducer? document)) (log/infof "Ignoring %s (%s)" product-number title)
          ;; validate the product number
          (not (s/valid? ::product-number product-number)) (throw (ex-info "The product-number is not valid" document))
          ;; If the product-number has been imported before just update the meta data of the existing document
          (documents/get-document-for-product-number product-number)
          (let [{:keys [id title] :as existing} (documents/get-document-for-product-number product-number)]
            (log/infof "Document %s (%s) for order number '%s' has already been imported." id title product-number)
            (update-document existing document))
          ;; If the book has been produced for another product, update the meta data of the existing document and
          ;; add the new product
          (documents/get-document-for-source-or-title-and-source-edition document)
          (let [{:keys [source title source-edition]} document
                {:keys [id title type] :as existing} (documents/get-document-for-source-or-title-and-source-edition document)]
            (log/infof "Document %s (%s) has already been imported for a different product." id title)
            (update-document existing document)
            (products/insert-product id product-number type))
          :else
          ;; the book has not been produced before, add the document using the given metadata and add the product
          (let [new (documents/initialize-document document)
                document-id (documents/insert-document new)]
            (log/infof "Document %s (%s) has not been imported before. Creating a document for %s." document-id title product-number)
            (products/insert-product document-id product-number type)
            (versions/insert-initial-version new))))
      (throw
       (ex-info "The provided xml is not valid"
                {:error-id :invalid-xml
                 :errors errors})))))

(prometheus/instrument! metrics/registry #'import-new-document)
