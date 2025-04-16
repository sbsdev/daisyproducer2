(ns daisyproducer2.documents.preview.dtbook2sbsform
  "Wrapper around the dtbook2sbsform scripts.

  It is assumed that the dtbook2sbsform scripts have been installed
  under `/opt/dtbook2sbsform`."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]
   [daisyproducer2.documents.utils :refer [with-tempfile]]
   [daisyproducer2.config :refer [env]]
   [sigel.xslt.core :as xslt]))

(def ^:private executable "/opt/dtbook2sbsform/dtbook2sbsform.sh")

(defn- log-process
  [proc]
  (log/info "Invoking" (:cmd proc)))

(defn- filter-implicit-headings
  [xml target]
  (let [xslt [(xslt/compile-xslt (io/resource "xslt/filter_implicit_headings.xsl"))]]
    (xslt/transform-to-file xslt (fs/file xml) (fs/file target))))

(defn- translate
  "Translate the given `dtbook` using given `args` and store it in
  `output-path`. If a `pipe` is also given use it to take the input of
  a pipe (typically from the hyphenation process)"
  ([dtbook output-path args]
   (translate nil dtbook output-path args))
  ([pipe dtbook output-path args]
   (with-tempfile [clean-xml {:prefix "daisyproducer-" :suffix "-clean.xml"}]
     (filter-implicit-headings dtbook clean-xml)
     (let [opts {:out :write
                 :out-file (fs/file output-path)
                 :err :string
                 :extra-env {"LANG" "en_US.UTF-8"}
                 :pre-start-fn log-process}
           input (if (nil? pipe) (format "-s:%s" clean-xml) "-s:-")]
       (try
         (if (nil? pipe)
           (apply process/shell opts executable input args)
           (apply process/shell pipe opts executable input args))
         (catch clojure.lang.ExceptionInfo e
           (log/error (ex-message e))
           (throw
            (ex-info "Braille translation failed" {:error-id ::braille-translation-failed} e))))))))

(defn- hyphenate
  [dtbook]
  (let [java (env :java17)
        opts {:out :string
              :err :string
              :extra-env {"LANG" "en_US.UTF-8"}
              :pre-start-fn log-process}]
    (try
      (process/shell opts java "-jar" "/usr/local/share/java/dtbook-hyphenator.jar" dtbook)
      (catch clojure.lang.ExceptionInfo e
        (log/error (ex-message e))
        (throw
         (ex-info "Hyphenation failed" {:error-id ::hyphenation-failed} e))))))

(defn- hyphenate-and-translate
  [dtbook output-path opts]
  (-> (hyphenate dtbook)
      (translate dtbook output-path opts)))

(defn- legacy-opt-values [opts]
  (let [mapping {:basic :de-accents
                 :swiss :de-accents-ch}]
    (update opts :detailed-accented-characters #(get mapping %1 %1))))

(defn- stringify-opts
  [[k v]]
  (let [opt-key (str/replace (name k) #"-" "_")]
    (cond
      (boolean? v) (format "?%s=%s" opt-key (if v "true()" "false()"))
      (integer? v) (format "?%s=%s" opt-key v)
      (keyword? v) (format "%s=%s" opt-key (name v))
      :else (format "%s=%s" opt-key v))))

(defn sbsform
  "Generate an SBSForm file for given `dtbook` into the file
  `output-path`. An exception is thrown if the document has no
  versions."
  [dtbook output-path opts]
  (let [hyphenation? (:hyphenation opts)
        opts (->> opts
                  legacy-opt-values
                  (map stringify-opts))]
    (if hyphenation?
      (hyphenate-and-translate dtbook output-path opts)
      (translate dtbook output-path opts))))
