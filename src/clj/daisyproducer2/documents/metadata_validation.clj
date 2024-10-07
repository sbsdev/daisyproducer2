(ns daisyproducer2.documents.metadata-validation
  "Validate the metadata of DTBook XML files"
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :refer [xml1-> attr= attr text]])
  (:import org.xml.sax.SAXParseException))

(defn- get-metadata-path
  "Return the path to the metadata for the given `field`"
  [field]
  [:head :meta (attr= :name field) (attr :content)])

(defn- get-element-path
  "Return the path to the element for the given `field`"
  [field]
  [:book :frontmatter field text])

(def ^:private param-mapping
  {:title [(get-metadata-path "dc:Title")]
   :author [(get-metadata-path "dc:Creator")]
   :subject [(get-metadata-path "dc:Subject")]
   :description [(get-metadata-path "dc:Description")]
   :publisher [(get-metadata-path "dc:Publisher")]
   :date [(get-metadata-path "dc:Date")]
   :identifier [(get-metadata-path "dc:Identifier")
                (get-metadata-path "dtb:uid")]
   :source [(get-metadata-path "dc:Source")]
   :language [[:head :meta (attr= :name "dc:Language") (attr :content)]
              [(attr :xml:lang)]]
   :rights [(get-metadata-path "dc:Rights")]
   :source-date [(get-metadata-path "dtb:sourceDate")]
   :source-edition [(get-metadata-path "dtb:sourceEdition")]
   :source-publisher [(get-metadata-path "dtb:sourcePublisher")]
   :source-rights [(get-metadata-path "dtb:sourceRights")]
   :production-series [(get-metadata-path "prod:series")]
   :production-series-number [(get-metadata-path "prod:seriesNumber")]
   :production-source [(get-metadata-path "prod:source")]
   })

(defn- get-path [loc path]
  (apply xml1-> loc path))

(defn- get-paths [loc paths]
  (map #(get-path loc %) paths))

(defn- path-valid?
  [loc path value]
  (let [xml-value (get-path loc path)
        db-value value]
    (= (str xml-value) (str db-value))))

(defn- paths-valid?
  "Validate `paths` in `loc` against a given `value`. Return true if
  all paths contain the `value`"
  [loc paths value]
  (every? #(path-valid? loc % value) paths))

(defn- error-message
  "Return an error message for `key` that has the value `expected`
  instead of the values in `actuals`"
  [key expected actuals]
  (let [name (str/capitalize (name key))]
    (str name " should be '" expected "' instead of "
         (str/join
          " and "
          (map #(if (some? %) (str "'" % "'") "undefined") 
               actuals)))))

(defn validate-metadata
  "Validate the meta data of a DTBook XML file against a given
  `production`. Return a sequence of error strings or an empty
  sequence if the XML in `dtbook` is valid"
  [dtbook production]
  (try
    (let [zipper (-> dtbook io/file xml/parse zip/xml-zip)]
      (for [[key paths] param-mapping
            :let [value (key production)]
            :when (not (paths-valid? zipper paths value))]
        (error-message key value (distinct (get-paths zipper paths)))))
    (catch SAXParseException e
      [(.getMessage e)])))
