(ns daisyproducer2.documents.abacus
  "Import files from ABACUS

  The interface to our ERP is crude: you basically read files from an
  directory. The import from ABACUS is done via XML. The interface
  imports the following notifications:

  - Start (open) a production."
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.data.xml :as data.xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer [xml-> xml1-> attr= attr text]]
            [clojure.string :as string]
            [daisyproducer2.documents.schema-validation :refer [validation-errors]]
            [iapetos.collector.fn :as prometheus]
            [daisyproducer2.metrics :as metrics]
            [clojure.string :as str]))

(def ^:private root-path [:Task :Transaction :DocumentData])

(def ^:private param-mapping
  {:product-number [:artikel_nr text]
   :title [:MetaData :dc :title text]
   :author [:MetaData :dc :creator text]
   :date [:MetaData :dc :date text]
   :source [:MetaData :dc :source text]
   :language [:MetaData :dc :language text]
   :source-publisher [:MetaData :ncc :sourcePublisher text]
   :source-edition [:MetaData :ncc :sourceDate text]
   :production-series-number [:MetaData :sbs :rucksackNr text]
   :reihe [:MetaData :sbs :reihe text]
   :aufwand [:MetaData :sbs :Aufwand_A2 text]
   :verkaufstext [:MetaData :sbs :verkaufstext text]
   })

(defn source-date
  "Extract the source date from a raw `production` by taking the last
  segment of the `:source-edition`"
  [{source-edition :source-edition}]
  (when source-edition
    (-> source-edition
        (string/split #"/")
        last
        (->> (re-find #"\d{4}")))))

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

(defn- production-type
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
  [{:keys [language source aufwand verkaufstext] :as raw-document}]
  (let [production-series (production-series raw-document)
        production-series-number (production-series-number raw-document)
        production-type (production-type raw-document)
        source-date (source-date raw-document)]
    (-> raw-document
        (dissoc :reihe :aufwand :verkaufstext)
        (cond-> source-date (assoc :source-date source-date))
        (assoc :publisher (get default-publisher language "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"))
        (cond-> (or (str/blank? source) (= source "keine")) (dissoc :source))
        (cond-> (= aufwand "D") (assoc :production-source "electronicData"))
        (cond-> production-type (assoc :production-type production-type))
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

(defn import-new-production
  "Import a new production from file `f`"
  [f]
  (let [errors (validation-errors f abacus-export-schema)]
    (if (empty? errors)
      #_(prod/create! (read-file f))
      (read-file f)
      (throw
       (ex-info "The provided xml is not valid"
                {:error-id :invalid-xml
                 :errors errors})))))

(prometheus/instrument! metrics/registry #'import-new-production)
