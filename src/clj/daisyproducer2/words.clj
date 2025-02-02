(ns daisyproducer2.words
  (:require [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [daisyproducer2.hyphenate :as hyphenate]
            [daisyproducer2.louis :as louis]
            [daisyproducer2.validation :as validation]))

(defn is-name? [{:keys [type]}] (some? (#{1 2} type)))
(defn is-place? [{:keys [type]}] (some? (#{3 4} type)))
(defn is-homograph? [{:keys [type]}] (some? (#{5} type)))

(defn suggested-hyphenation [{:keys [untranslated spelling]}]
  (hyphenate/hyphenate untranslated spelling))

(defn complement-hyphenation
  "Add hyphenation to a `word` if it is missing"
  [{:keys [hyphenated] :as word}]
  (let [hyphenation (suggested-hyphenation word)
        valid? (validation/hyphenation-valid? hyphenation)]
    (cond-> word
      (and (nil? hyphenated) valid?) (assoc :hyphenated hyphenation)
      (and (nil? hyphenated) (not valid?)) (assoc :invalid-hyphenated hyphenation))))

(def braille-dummy-text "┊")

(defn- complement-string
  "Given a string `s`, prepend `start` and/or append `end` if it doesn't
  already start or end with `start` or `end` respectively. If `s` is
  nil it is simply returned."
  [s start end]
  (let [prepend #(str %2 %1)
        append #(str %1 %2)]
    (when (some? s)
      (cond-> s
        (not (str/starts-with? s start))
        (prepend start)
        (not (str/ends-with? s end))
        (append end)))))

(defn complement-ellipsis-braille
  "Add ellipsis to the braille of a `word` if it is missing. Depending
  on whether `:untranslated` starts or ends with the dummy text, the
  dummy text is also added to `:uncontracted` and `:contracted`."
  [{:keys [untranslated uncontracted contracted] :as word}]
  (let [starts-with-dummy? (str/starts-with? untranslated braille-dummy-text)
        ends-with-dummy? (str/ends-with? untranslated braille-dummy-text)
        uncontracted (cond-> uncontracted
                       starts-with-dummy?
                       (complement-string braille-dummy-text "")
                       ends-with-dummy?
                       (complement-string "" braille-dummy-text))
        contracted (cond-> contracted
                     starts-with-dummy?
                     (complement-string braille-dummy-text "")
                     ends-with-dummy?
                     (complement-string "" braille-dummy-text))]
    (cond-> word
      uncontracted (assoc :uncontracted uncontracted)
      contracted (assoc :contracted contracted))))

(defn complement-braille
  "Add braille to a `word` if it is missing. If any of `:uncontracted`
  or `:contracted` is nil then the correct braille is translated with
  louis and added to the word."
  [{:keys [untranslated homograph-disambiguation] :as word}]
  (let [params {:name (is-name? word) :place (is-place? word)}
        ;; for homographs we have to use the homograph-disambiguation
        ;; to get the braille
        untranslated (if (is-homograph? word)
                       (str/replace homograph-disambiguation "|" braille-dummy-text)
                       untranslated)]
    (try
      (cond-> word
        (and (contains? word :uncontracted) (nil? (:uncontracted word)))
        (assoc :uncontracted (louis/translate untranslated (louis/translator (louis/get-tables 1 params))))
        (and (contains? word :contracted) (nil? (:contracted word)))
        (assoc :contracted (louis/translate untranslated (louis/translator (louis/get-tables 2 params)))))
      ;; if there was a problem with translating the word, simply return the
      ;; word without the braille added. That will serve as an indication that
      ;; the braille translation failed
      (catch clojure.lang.ExceptionInfo e
        word))))

(defn grades [grade]
  (case (int grade) ; convert grade into a list of grades
    (1 2) [grade] ; for grade 1 and 2 the list contains just that grade
    0 [1 2])) ; grade 0 really means both grades

(def grade-to-keyword
  {1 :uncontracted
   2 :contracted})

(def hyphenation-keys [:untranslated :hyphenated :spelling
                       ;; when deleting a hyphenation we also need the
                       ;; document-id to be able to check for
                       ;; references in the local words
                       :document-id])

(def hyphenation-mapping {:untranslated :word
                          :hyphenated :hyphenation})

(def dictionary-keys [:untranslated :uncontracted :contracted :type :homograph-disambiguation
                      :document-id :islocal :isconfirmed])

(def dictionary-mapping {})

(defn to-db [word keys mapping]
  (-> word
      (select-keys keys)
      (rename-keys mapping)))

(defn- value-to-boolean [v]
  (case v
    1 true
    0 false
    v))

(defn- keys-to-boolean
  "Convert all 0 and 1 values in `word` to boolean values for all keys in `keys`"
  [word keys]
  (let [replaced (-> word
                     (select-keys keys)
                     (update-vals value-to-boolean))]
    (merge word replaced)))

(defn int-fields-to-boolean
  ;; MySQL doesn't always seem to return the right type for boolean
  ;; values. This function should fix that problem.
  [word]
  (keys-to-boolean word [:islocal :isignored :isconfirmed]))
