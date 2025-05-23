(ns daisyproducer2.documents.abacus
  "Import files from ABACUS

  The interface to our ERP is crude: you basically read an XML file
  containing the relevant information. The interface imports the
  following notifications:

  - Import a document."
  (:require [clojure.data.zip.xml :refer [text xml1->]]
            [clojure.java.io :as io]
            [clojure.spec.alpha :as s]
            [clojure.string :as str]
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
            [java-time.api :as time]))

;;; Specifications for document and product

;; used only partially at the moment, i.e. when importing the document
;; is not validated as a whole, only `source` and `product-number` are
;; validated individually

(defn- local-date? [x] (instance? java.time.LocalDate x))
(defn- local-date-time? [x] (instance? java.time.LocalDateTime x))

(s/def ::title (s/and string? (complement str/blank?)))
(s/def ::author (s/nilable string?))
(s/def ::subject (s/nilable string?))
(s/def ::description (s/nilable string?))
(s/def ::publisher string?)
(s/def ::date local-date?)
(s/def ::identifier (s/and string? (complement str/blank?)))
(s/def ::source (s/nilable (s/and string? (fn [s] (re-matches #"^SBS[0-9]{6}|(?:978-|979-)?\d{1,5}-\d{1,7}-\d{1,6}-[0-9xX]" s))))) ;; ISBN
(s/def ::language (s/and string? #(re-matches #"^(de|de-1901|de-CH|it|rm-sursilv|en)$" %)))
(s/def ::rights (s/nilable string?))
(s/def ::source-date (s/nilable local-date?))
(s/def ::source-edition (s/nilable string?))
(s/def ::source-publisher (s/nilable string?))
(s/def ::source-rights (s/nilable string?))
(s/def ::state-id pos-int?)
(s/def ::created-at local-date-time?)
(s/def ::modified-at local-date-time?)
(s/def ::production-series (s/nilable string?))
(s/def ::production-series-number (s/nilable string?))
(s/def ::production-source (s/nilable string?))

(s/def ::document
  (s/keys :req-un [::author ::title ::publisher ::date ::language
                   ::source-edition ::source-publisher ::source-date]
          :opt-un [::subject ::description ::source ::rights
                   ::source-rights ::production-series
                   ::production-series-number ::production-source
                   ;; the following keys are only given once the document is persisted in the db
                   ::created-at ::modified-at ::state-id
                   ::identifier]))

(s/def ::product-number (s/and string? #(re-matches #"^(PS|GD|EB|ET)\d{4,7}$" %)))
(s/def ::product-type (s/and int? #(<= 0 % 5)))

(s/def ::document-product
  (s/keys :req-un [::product-number ::product-type]))

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
         (str/includes? reihe "SJW")) "SJW"
    :else ""))

(defn- production-series-number
  [{:keys [production-series-number reihe]}]
  (cond
    (not= production-series-number "0") production-series-number
    (and (= production-series-number "0")
         (not (str/blank? reihe))
         (str/includes? reihe "SJW")) (re-find #"\d+" reihe)
    :else ""))

(defn- source
  [{:keys [source]}]
  (cond
    (str/blank? source) nil
    (= source "keine") nil
    :else source))

(defn- product-type
  [{:keys [product-number]}]
  (condp #(str/starts-with? %2 %1) product-number
    "PS" :braille
    "GD" :large-print
    "EB" :ebook
    "ET" :etext
    nil))

(defn- year->date [year]
  (when year
    (time/local-date (parse-long year))))

(defn- source-date
  "Extract the source date from a raw `production` by taking the last
  segment of the `:source-edition`"
  [{source-edition :source-edition}]
  (when (and source-edition (string? source-edition))
    (-> source-edition
        (str/split #"/")
        last
        (->> (re-find #"\d{4}"))
        year->date)))

(def ^:private default-publisher {"de" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                  "it" "Unitas - Associazione ciechi e ipovedenti della Svizzera italiana"})

(defn clean-raw-document
  "Return a proper document based on a raw document"
  [{:keys [language aufwand verkaufstext date daisyproducer?] :as raw-document}]
  (let [production-series-number (production-series-number raw-document)
        production-series (production-series raw-document)
        product-type (product-type raw-document)
        source (source raw-document)
        source-date (source-date raw-document)
        production-source (if (= aufwand "D") "electronicData" "")
        date (time/local-date date)]
    (-> raw-document
        (dissoc :reihe :aufwand :verkaufstext :production-series-number)
        (assoc :publisher (get default-publisher language "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"))
        (assoc :daisyproducer? (= daisyproducer? "ja"))
        (assoc :date date)
        (assoc :source source)
        (assoc :source-date source-date)
        (assoc :production-source production-source)
        (assoc :production-series production-series)
        (assoc :production-series-number production-series-number)
        (cond-> product-type (assoc :product-type product-type))
        (cond-> (not (str/blank? verkaufstext)) (assoc :author (-> verkaufstext (str/split #"\[xx\]") first str/trim)))
        (cond-> (not (str/blank? verkaufstext)) (assoc :title (-> verkaufstext (str/split #"\[xx\]") second str/trim))))))

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

(def ^:private relevant-metadata-keys #{:title :author :date :language :source
                                        :source-publisher :source-edition :source-date
                                        :production-series :production-series-number :production-source})

(defn metadata-changed?
  [old new]
  (let [old (select-keys old relevant-metadata-keys)
        new (select-keys new relevant-metadata-keys)]
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
  (not (s/valid? ::source source)))

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
