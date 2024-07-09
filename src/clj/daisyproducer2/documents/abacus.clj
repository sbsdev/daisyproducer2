(ns daisyproducer2.documents.abacus
  "Import files from ABACUS

  The interface to our ERP is crude: you basically read files from an
  directory. The import from ABACUS is done via XML. The interface
  imports the following notifications:

  - Import a document."
  (:require [clojure.data.zip.xml :refer [text xml1->]]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [conman.core :as conman]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.documents.documents :as documents]
            [daisyproducer2.documents.products :as products]
            [daisyproducer2.documents.schema-validation :refer [validation-errors]]
            [daisyproducer2.documents.versions :as versions]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]
            [java-time.api :as time]
            [medley.core :as medley]))

(s/def ::product-number (s/and string? #(re-matches #"^(PS|GD|EB|ET)\d{4,7}$" %)))
(s/def ::isbn (s/and string? (fn [s] (or (string/blank? s) (re-matches #"^SBS[0-9]{6}|(?:978-|979-)?\d{1,5}-\d{1,7}-\d{1,6}-[0-9xX]" s)))))

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
         (not (string/blank? reihe))
         (string/includes? reihe "SJW")) "SJW"
    :else ""))

(defn- production-series-number
  [{:keys [production-series-number reihe]}]
  (cond
    (not= production-series-number "0") production-series-number
    (and (= production-series-number "0")
         (not (string/blank? reihe))
         (string/includes? reihe "SJW")) (re-find #"\d+" reihe)
    :else ""))

(defn- source
  [{:keys [source]}]
  (cond
    (string/blank? source) ""
    (= source "keine") ""
    :else source))

(defn- product-type
  [{:keys [product-number]}]
  (condp #(string/starts-with? %2 %1) product-number
    "PS" :braille
    "GD" :large-print
    "EB" :ebook
    "ET" :etext
    nil))

(def ^:private default-publisher {"de" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                  "it" "Unitas - Associazione ciechi e ipovedenti della Svizzera italiana"})

(defn clean-raw-document
  "Return a proper document based on a raw document"
  [{:keys [language aufwand verkaufstext date daisyproducer?] :as raw-document}]
  (let [production-series-number (production-series-number raw-document)
        production-series (production-series raw-document)
        product-type (product-type raw-document)
        source (source raw-document)
        production-source (if (= aufwand "D") "electronicData" "")
        date (time/local-date date)]
    (-> raw-document
        (dissoc :reihe :aufwand :verkaufstext :production-series-number)
        (assoc :publisher (get default-publisher language "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"))
        (assoc :daisyproducer? (= daisyproducer? "ja"))
        (assoc :date date)
        (assoc :source source)
        (assoc :production-source production-source)
        (assoc :production-series production-series)
        (assoc :production-series-number production-series-number)
        (cond-> product-type (assoc :product-type product-type))
        (cond-> (not (string/blank? verkaufstext)) (assoc :author (-> verkaufstext (string/split #"\[xx\]") first string/trim)))
        (cond-> (not (string/blank? verkaufstext)) (assoc :title (-> verkaufstext (string/split #"\[xx\]") second string/trim))))))

(defn extract-value
  "Extract values from a `zipper` from an ABACUS export file for `key`"
  [zipper key]
  (let [path (key param-mapping)]
    (case key
      (apply xml1-> zipper (concat root-path path)))))

(defn read-xml
  "Read XML from ABACUS and return a map with all the data, i.e. a document"
  [xml]
  (let [zipper (-> xml zip/xml-zip)]
    (->>
     (for [key (keys param-mapping)
           :let [val (extract-value zipper key)]
           :when (some? val)]
       [key val])
      (into {})
      clean-raw-document)))

(def ^:private abacus-export-schema "schema/abacus_export.rng")

(defn read-file
  "Read a new document from file `f`. Returns the new document. Throws
  an exception if the given file is not valid according to the
  `abacus-export-schema`."
  [f]
  (let [errors (validation-errors f abacus-export-schema)]
    (when (not-empty errors)
      (throw
       (ex-info "The provided xml is not valid"
                {:error-id :invalid-xml
                 :errors errors})))
    (read-xml (-> f io/file xml/parse))) )

(defn- ignore-document
  [{:keys [product-number title]}]
  (log/infof "Ignoring %s (%s)" product-number title)
  {:status :ignored})

(def ^:private relevant-metadata-keys #{:title :author :date :source :source-date
                                        :source-publisher :source-edition
                                        :production-series :production-series-number :production-source})

(defn metadata-changed?
  [old new]
  (let [old (medley/remove-vals nil? (select-keys old relevant-metadata-keys))
        new (medley/remove-vals nil? (select-keys new relevant-metadata-keys))]
    (not= old new)))

(defn- update-document-and-version
  [old new]
  (if (metadata-changed? old new)
    (let [new (merge old new)]
      (log/infof "Updating %s due to changed meta data" (:id old))
      (documents/update-document-meta-data new)
      (versions/insert-updated-version new))
    (log/infof "No change in meta data for %s" (:id old))))

(defn- update-document
  [{:keys [product-number] :as import}]
  (let [{:keys [id title] :as existing} (documents/get-document-for-product-number product-number)]
    (log/infof "Document %s (%s) for order number '%s' has already been imported." id title product-number)
    (conman/with-transaction [db/*db*]
      (update-document-and-version existing import))
    {:document-id id :status :updated}))

(def ^:private product-type-to-type
  {:braille 0
   :large-print 1
   :ebook 2
   :etext 3
   })

(defn- update-document-and-product
  [{:keys [product-number product-type] :as import}]
  (let [{:keys [id title] :as existing} (documents/get-document-for-source-or-title-and-source-edition import)]
    (log/infof "Document %s (%s) has already been imported for a different product." id title)
    (conman/with-transaction [db/*db*]
      (update-document-and-version existing import)
      (products/insert-product id product-number (product-type-to-type product-type)))
    {:document-id id :status :updated}))

(defn- insert-document-and-product
  "Insert a new document, an initial version and an associated product. Returns the `id` of the new document"
  [{:keys [product-number product-type title] :as import}]
  (conman/with-transaction [db/*db*]
    (let [new (documents/initialize-document import)
          document-id (documents/insert-document new)
          new (assoc new :id document-id)]
      (log/infof "Document %s (%s) has not been imported before. Creating a document for %s." document-id title product-number)
      (products/insert-product document-id product-number (product-type-to-type product-type))
      (versions/insert-initial-version new)
      {:document-id document-id :status :created})))

(defn- invalid-isbn?
  [{:keys [source]}]
  (not (s/valid? ::isbn source)))

(defn- invalid-product?
  [{:keys [product-number]}]
  (not (s/valid? ::product-number product-number)))

(defn- product-seen-before?
  [{:keys [product-number]}]
  (some? (documents/get-document-for-product-number product-number)))

(defn- source-or-title-source-edition-seen-before?
  [document]
  (some? (documents/get-document-for-source-or-title-and-source-edition document)))

(defn import-new-document
  "Import a new `document`. Return the `id` of the new or updated document."
  [{:keys [product-number title daisyproducer?] :as import}]
  (cond
    ;; If the XML indicates that this product is not produced with Daisy Producer ignore this file
    (not daisyproducer?) (ignore-document import)
    ;; validate the source
    (invalid-isbn? import) (throw (ex-info "The source is not valid" {:error-id :invalid-isbn :document import :errors [(:source import)]}))
    ;; validate the product number
    (invalid-product? import) (throw (ex-info "The product-number is not valid" {:error-id :invalid-product-number :document import :errors [(:product-number import)] }))
    ;; If the product-number has been imported before just update the meta data of the existing document
    (product-seen-before? import) (update-document import)
    ;; If the book has been produced for another product, update the meta data of the existing document and
    ;; add the new product
    (source-or-title-source-edition-seen-before? import) (update-document-and-product import)
    ;; the book has not been produced before, add the document using the given metadata and add the product
    :else (insert-document-and-product import)))

(prometheus/instrument! metrics/registry #'import-new-document)
