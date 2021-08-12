(ns daisyproducer2.words
  (:require
       [daisyproducer2.i18n :refer [tr]]))

(def type-mapping {0 (tr [:type-none])
                   1 (tr [:type-name-hoffmann])
                   2 (tr [:type-name])
                   3 (tr [:type-place-langenthal])
                   4 (tr [:type-place])
                   5 (tr [:type-homograph])})

(defn spelling-string [spelling]
  (case spelling
    0 (tr [:old-spelling])
    1 (tr [:new-spelling])
    (tr [:unknown-spelling])))

(defn spelling-brief-string [spelling]
  (case spelling
    0 (tr [:old])
    1 (tr [:new])
    (tr [:unknown])))
