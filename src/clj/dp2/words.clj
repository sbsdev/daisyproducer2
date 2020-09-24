(ns dp2.words
  (:require
   [dp2.db.core :as db]
   [clojure.set :refer [difference]]
   [clojure.string :as string]
   [sigel.xslt.core :as xslt]
   [sigel.xpath.core :as xpath]
   [dp2.louis :as louis]
   [dp2.hyphenate :as hyphenate]))

(def compiler
  (-> (xpath/compiler)
      (xpath/set-default-namespace! "http://www.daisy.org/z3986/2005/dtbook/")
      (xpath/declare-namespace! "brl" "http://www.daisy.org/z3986/2009/braille/")))

(defn name? [type] (#{1 2} type))
(defn place? [type] (#{3 4} type))

(defn compare-with-known-words
  "Given a set of `words` return the ones that are unknown."
  ([words document_id grade]
   (compare-with-known-words words document_id grade db/get-all-known-words))
  ([words document_id grade query-fn]
   (if (empty? words)
     ;; if there are no words then none of them can be unknown.
     ; The db query doesn't like an empty word list
     #{}
     (let [known-words
           (->>
            (query-fn {:document_id document_id :grade grade :words words})
            (map :untranslated)
            set)]
       (difference words known-words)))))

(defn compare-with-known-homographs
  "Given a set of `homographs` return the ones that are unknown."
  [homographs document_id grade]
  (compare-with-known-words homographs document_id grade db/get-all-known-homographs))

(defn compare-with-known-names
  "Given a set of `names` return the ones that are unknown."
  [names document_id grade]
  (compare-with-known-words names document_id grade db/get-all-known-names))

(defn compare-with-known-places
  "Given a set of `places` return the ones that are unknown."
  [places document_id grade]
  (compare-with-known-words places document_id grade db/get-all-known-places))

(defn filter-braille
  [xml]
  (xslt/transform (xslt/compile-xslt "resources/xslt/filter.xsl") xml))

(defn filter-braille-and-names
  [xml]
  (let [xslt [(xslt/compile-xslt "resources/xslt/filter.xsl")
              (xslt/compile-xslt "resources/xslt/filter_names.xsl")
              (xslt/compile-xslt "resources/xslt/to_string.xsl")]]
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
   (filter valid-char?)
   (string/join)))

(defn- extract-xpath [xml xpath]
  (->>
   (xpath/select compiler xml xpath {})
   (map (comp str string/lower-case))
   set))

(defn extract-homographs [xml]
  (extract-xpath xml "//brl:homograph/text()"))

(defn extract-names [xml]
  (extract-xpath xml "//brl:name/text()"))

(defn extract-places [xml]
  (extract-xpath xml "//brl:place/text()"))

(defn extract-words [text]
  (->>
   (string/split (filter-text text) #"(?U)\W")
   ;; drop words shorter than 3 chars
   (remove (fn [word] (< (count word) 3)))
   (map string/lower-case)
   set))

(defn embellish-words [words document_id grade type]
  (let [template {:document_id document_id
                  :type type
                  :grade grade}]
    (map (fn [untranslated word]
           (let [tables (louis/get-tables grade {:name (name? type)
                                                 :place (place? type)})
                 braille (louis/translate untranslated tables)
                 hyphenated (hyphenate/hyphenate untranslated)]
             (assoc word
                    :untranslated untranslated :braille braille
                    :hyphenated hyphenated)))
         words (repeat template))))

(comment
  ;; extract words from an dtbook file
  (require '[clojure.java.io :as io])
  
  (let [f (io/file "/home/eglic/tmp/6747.xml")
        content (str (filter-braille-and-names f))]
    (extract-words content))
  
  ;; extract unknown words from an dtbook file
  (let [f (io/file "/home/eglic/tmp/6747.xml")
        content (str (filter-braille-and-names f))
        words (extract-words content)
        document_id 644
        grade 2]
    (compare-with-known-words words document_id grade))
  
  )
