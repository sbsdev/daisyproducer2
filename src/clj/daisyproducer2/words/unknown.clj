(ns daisyproducer2.words.unknown
  (:require [clojure.java.io :as io]
            [clojure.set :refer [union]]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [conman.core :as conman]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.words :as words]
            [iapetos.collector.fn :as prometheus]
            [sigel.xpath.core :as xpath]
            [sigel.xslt.core :as xslt]))

(def compiler
  (-> (xpath/compiler)
      (xpath/set-default-namespace! "http://www.daisy.org/z3986/2005/dtbook/")
      (xpath/declare-namespace! "brl" "http://www.daisy.org/z3986/2009/braille/")))

(defn filter-braille
  [xml]
  (xslt/transform (xslt/compile-xslt (io/resource "xslt/filter.xsl")) xml))

(defn filter-braille-and-names
  [xml]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filter.xsl"))
              (xslt/compile-xslt (io/resource "xslt/filter_names.xsl"))
              (xslt/compile-xslt (io/resource "xslt/to_string.xsl"))]]
    (xslt/transform xslt xml)))

(def valid-character-types
  #{Character/LOWERCASE_LETTER Character/UPPERCASE_LETTER
    Character/SPACE_SEPARATOR Character/LINE_SEPARATOR Character/PARAGRAPH_SEPARATOR
    Character/DASH_PUNCTUATION Character/OTHER_PUNCTUATION})

(defn- valid-char? [char]
  (or (contains? valid-character-types (Character/getType ^Character char))
      (contains? #{\newline \return} char)))

(defn filter-text [text]
  (->>
   (sequence text)
   ;; drop everything that is not a letter, space or punctuation
   (map #(if (valid-char? %) % " "))
   (str/join)))

(defn- extract-xpath [xml xpath]
  (->>
   (xpath/select compiler xml xpath {})
   (map (comp str str/lower-case))
   set))

(defn extract-homographs [xml]
  (extract-xpath xml "for $h in (//brl:homograph) return string-join($h/text(),'|')"))

(defn extract-names [xml]
  (extract-xpath xml "//brl:name/text()"))

(defn extract-places [xml]
  (extract-xpath xml "//brl:place/text()"))

(def ellipsis-re #"(?U)\.{3}[\p{Alpha}']{2,}|[\p{Alpha}']{2,}\.{3}")

(defn extract-re [xml re to-replace]
  (->> xml
   str
   (re-seq re)
   (map str/lower-case)
   (map #(str/replace % to-replace words/braille-dummy-text))
   set))

(defn extract-ellipsis-words [xml]
  (extract-re xml ellipsis-re "..."))

(def supplement-hyphen-re #"(?U)\B-[\p{Alpha}']{2,}|[\p{Alpha}']{2,}-\B")

(defn extract-hyphen-words [xml]
  (extract-re xml supplement-hyphen-re "-"))

(defn extract-special-words [xml]
  (union
   (extract-ellipsis-words xml)
   (extract-hyphen-words xml)))

(defn filter-special-words [text]
  (-> text
      (str/replace ellipsis-re "")
      (str/replace supplement-hyphen-re "")))

(defn extract-words [xml]
  (-> xml
   str
   filter-special-words
   filter-text
   (str/split #"(?U)[^\w']")
   (->>
    ;; drop words shorter than 3 chars
    (remove (fn [word] (< (count word) 3)))
    (map str/lower-case)
    set)))

(def max-word-length 100)
(defn- drop-overly-long-words [words]
  (remove #(> (count %) max-word-length) words))

(defn get-names
  [xml document-id]
  (let [words (-> xml filter-braille extract-names drop-overly-long-words)
        tuples (map (fn [w] [w 2 "" document-id]) words)]
    tuples))

(defn get-places
  [xml document-id]
  (let [words (-> xml filter-braille extract-places drop-overly-long-words)
        tuples (map (fn [w] [w 4 "" document-id]) words)]
    tuples))

(defn get-homographs
  [xml document-id]
  (let [words (-> xml filter-braille extract-homographs drop-overly-long-words)
        tuples (map (fn [w] [(str/replace w "|" "") 5 w document-id]) words)]
    tuples))

(defn get-plain
  [xml document-id]
  (let [filtered (-> xml filter-braille-and-names)
        special-words (-> filtered extract-special-words) ; ellipsis and hyphen
        plain-words (-> filtered extract-words)
        all-words (-> (union plain-words special-words)
                      drop-overly-long-words)
        tuples (map (fn [w] [w 0 "" document-id]) all-words)]
    tuples))

(defn update-words
  "Update the list of unknown words in the \"temporary\" table for
  given `document-id` and the new content in `xml`"
  [xml document-id]
  (let [new-words (concat
                   (get-names xml document-id)
                   (get-places xml document-id)
                   (get-homographs xml document-id)
                   (get-plain xml document-id))]
    (if (empty? new-words)
      [] ; if there are no new words there are no unknown words
      (conman/with-transaction [db/*db*]
        (db/delete-unknown-words {:document-id document-id})
        (db/insert-unknown-words {:words new-words})
        (let [deleted (db/delete-non-existing-unknown-words-from-local-words
                         {:document-id document-id})]
            (log/infof "Deleted %s local words that were not in unknown words for book %s"
                       deleted document-id))))))

(defn get-words
  "Retrieve all unknown words for given document-id `id` and `grade`.
  Limit the result set by `limit` and `offset`."
  [document-id grade limit offset]
  (->>
   (db/get-all-unknown-words
    {:document-id document-id :grade grade :limit limit :offset offset})
   (map words/int-fields-to-boolean)
   (map words/complement-braille)
   (map words/complement-ellipsis-braille)
   (map words/complement-hyphenation)))

(defn put-word
  "Update the unknown `word` in the db. Returns the number of updates."
  [word]
  (log/debug "Update unknown word" word)
  (let [dictionary-keys (conj words/dictionary-keys :isignored)]
    (db/update-unknown-word (words/to-db word dictionary-keys words/dictionary-mapping))))

(prometheus/instrument! metrics/registry #'update-words)
(prometheus/instrument! metrics/registry #'get-words)
(prometheus/instrument! metrics/registry #'put-word)
