(ns daisyproducer2.test.statistics-test
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [daisyproducer2.words.statistics :as statistics]))

(deftest statistics->csv
  (let [csv (statistics/statistics->csv
             [{:date (java.time.LocalDateTime/of 2026 10 19 9 5 0) :document-id 7
               :title "Märchen; \"neu\"" :author "Gorkij, Maksim" :source "3-85580-169-X"
               :language "de" :grade 2 :total 4713 :unknown 437}
              ;; statistics of a deleted document
              {:date (java.time.LocalDateTime/of 2013 3 6 15 50 26) :document-id nil
               :grade 1 :total 10 :unknown 0}])
        [header first-row second-row :as lines] (str/split (subs csv 1) #"\r\n")]
    (testing "byte order mark, so that Excel reads the umlauts as UTF-8"
      (is (str/starts-with? csv "\uFEFF")))
    (is (= 3 (count lines)))
    (is (= "date;document-id;title;author;source;language;grade;total;unknown" header))
    (is (= "2026-10-19 09:05:00;7;\"Märchen; \"\"neu\"\"\";Gorkij, Maksim;3-85580-169-X;de;2;4713;437" first-row))
    (is (= "2013-03-06 15:50:26;;;;;;1;10;0" second-row))))
