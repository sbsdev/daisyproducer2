(ns daisyproducer2.test.abacus-import.properties-test
  (:require
   [clojure.data.xml :as xml]
   [clojure.string :as string]
   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.clojure-test :as chuck]
   [com.gfredericks.test.chuck.generators :as chuck-gen]
   [daisyproducer2.documents.abacus :refer :all]
   [java-time.api :as time]))

(defrecord Raw [product-number title creator language source date
                source-publisher source-edition
                production-series-number reihe aufwand daisy-producer])
(defrecord Imported [product-number product-type
                     title author publisher date source language
                     source-publisher source-edition
                     production-series production-series-number production-source
                     daisyproducer?])

(def sbs-identifier (chuck-gen/string-from-regex #"SBS[0-9]{6}"))
(def isbn (chuck-gen/string-from-regex #"(978-|979-)?[0-9]{1,5}-[0-9]{1,7}-[0-9]{1,6}-[0-9xX]"))
(def sbs-isbn (gen/one-of [(gen/return "keine") sbs-identifier isbn]))
(def product-number (chuck-gen/string-from-regex #"(PS|GD|EB|ET)[0-9]{4,7}"))
(def language (gen/elements ["de" "de-CH" "it" "rm-sursilv"]))
(def aufwand (gen/elements ["" "D"]))
(def daisy_producer (gen/elements ["ja" "nein"]))
(def reihe (chuck-gen/string-from-regex #"SJW[0-9]+"))
(defn- valid-date-tuple? [[year month day]] (try (time/local-date year month day) true (catch Exception e false)))
(def date (gen/fmap (fn [[year month day]] (format "%d-%02d-%02d" year month day))
                    (gen/such-that valid-date-tuple? (gen/tuple (gen/choose 1900 2500) (gen/choose 1 12) (gen/choose 1 31)))))

(def raw-gen (gen/tuple product-number gen/string gen/string language sbs-isbn date (gen/return "") (gen/return "") gen/nat reihe aufwand daisy_producer))

(defn- xml-sample
  [{:keys [product-number title creator source language date
           source-publisher source-edition
           production-series-number reihe aufwand daisy-producer]}]
  [:AbaConnectContainer
   [:Task
    [:Transaction
     [:DocumentData
      [:artikel_nr product-number]
      [:title title]
      [:MetaData
       [:dc
        [:title title]
        [:creator creator]
        [:source source]
        [:language language]
        [:date date]]
       [:sbs
        [:verlag source-publisher]
        [:auflageJahr source-edition]
        [:rucksackNr production-series-number]
        [:reihe reihe]
        [:daisy_producer daisy-producer]
        [:Aufwand_A2 aufwand]]]]]]])

(defn- normalize-whitespace
  [s]
  (string/replace s (re-pattern (str "[\\s\u00A0]+")) " "))

(deftest abacus-import-with-properties
  (chuck/checking "ABACUS import is correct" 200
    [sample raw-gen]
    (let [input (apply ->Raw sample)
          imported (read-xml (xml/sexp-as-element (xml-sample input)))]
      (is (:source imported))
      (is (#{:braille :large-print :ebook :etext} (:product-type imported)))
      (is (#{"" "PPP" "SJW"} (:production-series imported)))
      (is (#{"de" "de-CH" "it" "rm-sursilv"} (:language imported)))
      ;; the xml import normalizes whitespace, so to compare actual
      ;; and expected we also have to normalize
      (is (= (normalize-whitespace (:creator input)) (:author imported)))
      (is (= (normalize-whitespace (:title input)) (:title imported)))
      ;; either the production-series-number is <> 0 and then the production-series is PPP or the
      ;; production-series-number is = 0 and then the production-series is SJW or the
      ;; production-series-number is empty
      (is (or (and (not= (:production-series-number imported) 0) (= (:production-series imported) "PPP"))
              (and (= (:production-series-number imported) 0) (= (:production-series imported) "SJW"))
              (= (= (:production-series-number imported) "")))))))


(deftest meta-data-changed
  (chuck/checking "The metadata is different" 200
    [old-sample raw-gen
     new-sample raw-gen]
    (let [old (read-xml (xml/sexp-as-element (xml-sample (apply ->Raw old-sample))))
          new (read-xml (xml/sexp-as-element (xml-sample (apply ->Raw new-sample))))]
      (is (metadata-changed? old new)))))


