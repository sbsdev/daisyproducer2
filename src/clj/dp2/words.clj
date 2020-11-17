(ns dp2.words)

(defn spelling [language]
  (case language
    ("de" "de-CH") 1
    ("de-1901" "de-CH-1901") 0
    1))

(defn grades [grade]
  (case grade ; convert grade into a list of grades
    (1 2) [grade] ; for grade 1 and 2 the list contains just that grade
    0 [1 2])) ; grade 0 really means both grades

(defn grade-to-keyword [grade]
  (keyword (str "grade" grade)))

(defn merge-words
  "Merge a seq of `words` into one. Presumably the words are for
  different grades. All keys are merged as usual except for `:braille`
  which will be merged to `:grade1` or `grade2`, depending on the
  grade of the original words."
  [words]
  (reduce (fn [m {:keys [grade braille islocal] :as word}]
            (-> m
                ;; Merge everything but grade, braille and islocal.
                ;; These will be added separately
                (merge (dissoc word :grade :braille :islocal))
                (assoc (grade-to-keyword grade) braille)
                ;; we assume that if any of the grades are local then
                ;; the whole word is local
                (update :islocal #(or %1 %2) islocal)))
          {} words))

(defn aggregate
  "Given a seq of `words` with distinct entries for each `:untranslated`
  and `:grade,` aggregate them into a seq with distinct entries for
  each `:untranslated` but aggregate the grades into that one entry.
  So
  `[{:untranslated \"foo\" :grade 1 :braille \"FOO\"} {:untranslated \"foo\" :grade 2 :braille \"F4\"}]`
  becomes
  [{:untranslated \"foo\" :grade1 \"FOO\" :grade2 \"F4\"]"
  [words]
  (->> words
       (group-by (juxt :untranslated :type :homograph-disambiguation))
       vals
       (map merge-words)))

