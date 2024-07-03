(ns daisyproducer2.test.abacus-import-test
  (:require
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.string :as string]
   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [com.gfredericks.test.chuck.generators :as chuck-gen]
   [com.gfredericks.test.chuck.clojure-test :as chuck]
   [daisyproducer2.documents.abacus :refer :all]
   [java-time.api :as time]))

(defrecord Raw [product-number title creator language source date source-publisher source-edition production-series-number reihe aufwand daisyproducer?])
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

(def raw-gen (gen/tuple
              product-number
              gen/string
              gen/string
              language
              sbs-isbn
              date
              (gen/return "")
              (gen/return "")
              gen/nat
              reihe
              aufwand
              daisy_producer
              ))

(defn- xml-sample
  [{:keys [product-number title creator source language date source-publisher source-edition production-series-number reihe aufwand daisyproducer?]}]
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
        [:daisy_producer (if daisyproducer? "ja" "nein")]
        [:Aufwand_A2 aufwand]]]]]]])

(deftest abacus-import
  (testing "ABACUS import"

    (testing "Read XML"
      (let [document (->Imported "EB11111" :ebook "Eine für de Thesi" "Gwerder, Anna" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                 (time/local-date "2011-12-23") "" "de" "DVA" "1. / 2011" "" "" "" false)]
        (are [expected actual] (= (into {} expected) (read-xml (xml/sexp-as-element (xml-sample actual))))
          document (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" false)
          (assoc document :daisyproducer? true) (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" true)
          (assoc document :production-series-number "500" :production-series "PPP")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 500 "" "" false)
          (assoc document :production-series-number "7000" :production-series "SJW")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "SJW 7000" "" false)
          (assoc document :product-number "GD11111" :product-type :large-print)
          (->Raw "GD11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" false)
          (assoc document :product-number "PS11111" :product-type :braille)
          (->Raw "PS11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" false)
          (assoc document :production-source "electronicData")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "D" false)
          document
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "C" false))))))

(deftest abacus-import-file
  (testing "ABACUS import"

    (testing "Read a file"
      (let [sample (io/file (io/resource "SN_Alfresco_EB11111.xml"))]
        (is (= (into {} (->Imported "EB11111" :ebook "Eine für de Thesi" "Gwerder, Anna" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                    (time/local-date "2011-12-23") "" "de" "DVA" "1. / 2011" "" "" "electronicData" true))
               (import-new-document-file sample)))))))

(deftest abacus-import-with-properties
  (chuck/checking "ABACUS import is correct" 200
    [sample raw-gen]
    (let [input (apply ->Raw sample)
          imported (read-xml (xml/sexp-as-element (xml-sample input)))]
      (is (:source imported))
      (is (#{:braille :large-print :ebook :etext} (:product-type imported)))
      (is (#{"" "PPP" "SJW"} (:production-series imported)))
      #_(is (= (:creator input) (:author imported))))))


