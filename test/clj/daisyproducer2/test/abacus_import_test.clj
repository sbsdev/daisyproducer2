(ns daisyproducer2.test.abacus-import-test
  (:require
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [daisyproducer2.documents.abacus :refer :all]
   [java-time.api :as time]
   [clojure.data.xml :as xml]))

(defrecord Document [product-number title creator source language date production-series-number reihe aufwand daisyproducer?])

(defn- xml-sample
  [{:keys [product-number title creator source language date production-series-number reihe aufwand daisyproducer?]}]
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
        [:rucksackNr production-series-number]
        [:reihe reihe]
        [:daisy_producer (if daisyproducer? "ja" "nein")]
        [:Aufwand_A2 aufwand]]]]]]])

(deftest abacus-import
  (testing "ABACUS import"

    (testing "Read XML"
      (let [document {:product-number "EB11111"
                      :title "Eine für de Thesi"
                      :author "Gwerder, Anna"
                      :language "de"
                      :date (time/local-date "2011-12-23")
                      :publisher "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                      :daisyproducer? false
                      :product-type :ebook}]
        (are [expected actual] (= expected (read-xml (xml/sexp-as-element (xml-sample actual))))
          document (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "" false)
          (assoc document :daisyproducer? true) (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "" true)
          (assoc document :production-series-number "500" :production-series "PPP")
          (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 500 "" "" false)
          (assoc document :production-series-number "7000" :production-series "SJW")
          (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "SJW 7000" "" false)
          (assoc document :product-number "GD11111" :product-type :large-print)
          (->Document "GD11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "" false)
          (assoc document :product-number "PS11111" :product-type :braille)
          (->Document "PS11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "" false)
          (assoc document :production-source "electronicData")
          (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "D" false)
          document
          (->Document "EB11111" "Eine für de Thesi" "Gwerder, Anna" "" "de" "2011-12-23" 0 "" "C" false))))

    (testing "Read a file"
      (let [sample (io/file (io/resource "SN_Alfresco_EB11111.xml"))]
        (is (= {:source-publisher "DVA"
                :date (time/local-date "2011-12-23")
                :source-edition "1. / 2011"
                :publisher "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
                :product-number "EB11111"
                :title "Eine für de Thesi"
                :author "Gwerder, Anna"
                :production-source "electronicData"
                :product-type :ebook
                :language "de"
                :daisyproducer? true}
               (import-new-document-file sample)))))))
