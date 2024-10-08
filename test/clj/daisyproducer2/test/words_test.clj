(ns daisyproducer2.test.words-test
  (:require
   [clojure.test :refer :all]
   [daisyproducer2.words.unknown :refer :all]
   [mount.core :as mount]))


(deftest test-words
  (testing "Unknown words"

    (testing "Extracting supplement hyphen words"
      (is (= (extract-hyphen-words "Hallo- wie geh't heute oder -morgen aber nicht-so-schnell-")
             #{"hallo┊" "┊morgen" "schnell┊"}))

      (is (= (extract-hyphen-words "geht's- noch schneller")
             #{"geht's┊"})))

    (is (= (extract-hyphen-words "30- und 40-jährige")
           #{}))

    (is (= (extract-hyphen-words "-50 Grad")
           #{}))

    (testing "Extracting ellipsis words"
      (is (= (extract-ellipsis-words "...HĘllo Leute ...wie gehts'... euch hEute...")
             #{"┊hęllo" "┊wie" "gehts'┊" "heute┊"})))

    (testing "Filtering special words"
      (is (= " Leute   euch  wahrlich  äh, nein gross-artig"
             (filter-special-words "...HĘllo Leute ...wie gehts'... euch hEute... wahrlich gross- äh, nein gross-artig"))))

    (testing "Extracting plain words"
      (is (= (extract-words "...HĘllo Leute ...wie gehts'... euch hEute... wahrlich gross- äh, nein gross-artig")
             #{"wahrlich" "gross" "artig" "euch" "leute" "nein"}))

      (is (= (extract-words "worta-wortb- und -wortc-wortd")
             #{"und" "worta" "wortd"})))

    (testing "Words with apos"
      (is (= (extract-words "hat's noch was schön's?")
             #{"hat's" "noch" "was" "schön's"}))

      (is (= (extract-words
              "fort's hentong's l'avalanche s'indeer s'baldio'z 'opkins kön'ge")
             #{"s'indeer" "s'baldio'z" "l'avalanche" "kön'ge" "'opkins" "hentong's" "fort's"})))))
