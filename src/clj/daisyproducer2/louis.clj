(ns daisyproducer2.louis
  (:require [clojure.tools.logging :as log])
  (:import (org.liblouis Translator)))

(def base-tables ["sbs-wordsplit.dis" "sbs-de-core6.cti" "sbs-de-accents.cti",
                  "sbs-special.cti" "sbs-whitespace.mod" "sbs-de-letsign.mod"
                  "sbs-numsign.mod" "litdigits6Dots.uti" "sbs-de-core.mod"])

(def grade1-tables (conj base-tables "sbs-de-g1-core.mod" "sbs-special.mod"))

(def grade2-tables (conj base-tables "sbs-de-g2-core.mod" "sbs-special.mod"))

(def grade2-name-tables (conj base-tables "sbs-de-g2-name.mod" "sbs-special.mod"))

(def grade2-place-tables (conj base-tables "sbs-de-g2-place.mod" "sbs-de-g2-name.mod"
                               "sbs-special.mod"))

(defn get-tables
  ([grade]
   (get-tables grade {}))
  ([grade {:keys [place name]}]
   (cond
     (= grade 1) grade1-tables
     place grade2-place-tables
     name grade2-name-tables
     :else grade2-tables)))

(defn translator [tables]
  (let [tables-string (apply str (interpose \, tables))]
    (Translator. ^String tables-string)))

(defn translate [word translator]
  (let [length (count word)
        inter-character-attributes (int-array (repeat (- length 1) 0))]
    (try
      (.getBraille (.translate translator word nil nil inter-character-attributes))
      ;; log the params that caused the exception and bubble it up
      (catch Exception e
        (log/errorf "Translation failed for word '%s' with tables %s: %s" word (str translator) e)
        (throw e)))))
