(ns daisyproducer2.test.abacus-import-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [daisyproducer2.documents.abacus :refer :all]
   [java-time.api :as time]
   [clojure.data.xml :as xml]))

(defrecord Raw [product-number title creator language source date source-publisher source-edition production-series-number reihe aufwand daisyproducer?])
(defrecord Imported [product-number product-type
                     title author publisher date source language
                     source-publisher source-edition
                     production-series production-series-number production-source
                     daisyproducer?])

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
