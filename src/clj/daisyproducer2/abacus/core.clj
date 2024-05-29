(ns daisyproducer2.abacus.core
  "Import files from ABACUS

  The import from ABACUS is done via XML. The interface imports the following
  notifications:

  - Start (open) a production."
  (:require [clojure.java.io :as io]
            [clojure.xml :as xml]
            [clojure.data.xml :as data.xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer [xml-> xml1-> attr= attr text]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [daisyproducer2.config :refer [env]]
            [daisyproducer2.abacus.validation :as validation]
            [iapetos.collector.fn :as prometheus]
            [daisyproducer2.metrics :as metrics]))

(def ^:private root-path [:Task :Transaction :DocumentData])

(def ^:private param-mapping
  {:product_number [:artikel_nr text]
   :title [:MetaData :dc :title text]
   :creator [:MetaData :dc :creator text]
   :date [:MetaData :dc :date text]
   :source [:MetaData :dc :source text]
   :language [:MetaData :dc :language text]
   :source_publisher [:MetaData :ncc :sourcePublisher text]
   :source_edition [:MetaData :ncc :sourceDate text]
   :narrator [:MetaData :ncc :narrator text]
   :volumes [:MetaData :ncc :setInfo text]
   :revision_date [:MetaData :ncc :revisionDate text]
   :library_record_id [:MetaData :ncc :VubisRecordID text]
   :mvl_only [:mvl_only text]
   :command [(attr :command)]
   :idVorstufe [:MetaData :sbs :idVorstufe text]
   })

(defn source-date
  "Extract the source date from a raw `production` by taking the last
  segment of the `:source_edition`"
  [{source_edition :source_edition}]
  (when source_edition
    (-> source_edition
        (string/split #"/")
        last
        (->> (re-find #"\d{4}")))))

(defn clean-raw-production
  "Return a proper production based on a raw production, i.e. drop
  `:mvl_only`, `:command` and `:idVorstufe` and add `:production_type`
  and `:periodical_number`"
  [{:keys [mvl_only command idVorstufe] :as raw-production}]
  (let [production_type (cond
                          (= command "mdaDocAdd_Kleinauftrag") "other"
                          (= mvl_only "yes") "periodical"
                          :else "book")
        source-date (source-date raw-production)]
    (-> raw-production
        (dissoc :mvl_only :command :idVorstufe)
        (assoc :production_type production_type)
        (cond-> source-date (assoc :source_date source-date))
        (cond-> (= production_type "periodical")
          (assoc :periodical_number idVorstufe)))))

(defn extract-value
  "Extract values from a `zipper` from an ABACUS export file for `key`"
  [zipper key]
  (let [path (key param-mapping)]
    (case key
      :narrator (string/join "; " (apply xml-> zipper (concat root-path path)))
      (apply xml1-> zipper (concat root-path path)))))

(defn read-file
  "Read an export file from ABACUS and return a map with all the data,
  i.e. a production"
  [file]
  (let [zipper (-> file io/file xml/parse zip/xml-zip)]
    (->>
     (for [key (keys param-mapping)
           :let [val (extract-value zipper key)]
           :when (some? val)]
       [key val])
      (into {})
      clean-raw-production
      #_prod/parse)))

(defn import-new-production
  "Import a new production from file `f`"
  [f]
  (let [errors (validation/abacus-validation-errors f)]
    (if (empty? errors)
      #_(prod/create! (read-file f))
      (throw
       (ex-info "The provided xml is not valid"
                {:error-id :invalid-xml
                 :errors errors})))))


(prometheus/instrument! metrics/registry #'import-new-production)
