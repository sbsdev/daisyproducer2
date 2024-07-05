(ns daisyproducer2.test.abacus-import.core-test
  (:require
   [clojure.data.xml :as xml]
   [clojure.java.io :as io]
   [clojure.test :refer :all]
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

(deftest abacus-read-xml
  (testing "ABACUS import"

    (testing "Read XML"
      (let [document (->Imported "EB11111" :ebook "Eine für de Thesi" "Gwerder, Anna" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                 (time/local-date "2011-12-23") "" "de" "DVA" "1. / 2011" "" "" "" false)]
        (are [expected actual] (= (into {} expected) (read-xml (xml/sexp-as-element (xml-sample actual))))
          document (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "nein")
          (assoc document :daisyproducer? true) (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "ja")
          (assoc document :production-series-number "500" :production-series "PPP")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 500 "" "" "nein")
          (assoc document :production-series-number "7000" :production-series "SJW")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "SJW 7000" "" "nein")
          (assoc document :product-number "GD11111" :product-type :large-print)
          (->Raw "GD11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "nein")
          (assoc document :product-number "PS11111" :product-type :braille)
          (->Raw "PS11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "nein")
          (assoc document :production-source "electronicData")
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "D" "nein")
          document
          (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "C" "nein"))))))

(deftest abacus-read-file
  (testing "ABACUS import"

    (testing "Read a file"
      (let [sample (io/file (io/resource "SN_Alfresco_EB11111.xml"))]
        (is (= (into {} (->Imported "EB11111" :ebook "Eine für de Thesi" "Gwerder, Anna" "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                                    (time/local-date "2011-12-23") "" "de" "DVA" "1. / 2011" "" "" "electronicData" true))
               (read-file sample)))))))

(deftest abacus-import-document
  (testing "ABACUS import"

    (testing "Validation checks throw exceptions"

      (let [sample (-> (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "ja")
                       xml-sample
                       xml/sexp-as-element
                       read-xml)]
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"The source is not valid" (import-new-document (assoc sample :source "faulty"))))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo #"The product-number is not valid" (import-new-document (assoc sample :product-number "faulty"))))))

    (testing "Ignore documents which aren't meant for daiyproducer"

      (let [sample (-> (->Raw "EB11111" "Eine für de Thesi" "Gwerder, Anna" "de" "" "2011-12-23" "DVA" "1. / 2011" 0 "" "" "nein")
                       xml-sample
                       xml/sexp-as-element
                       read-xml)]
        (is (= nil (import-new-document (assoc sample :source "faulty"))))))))



