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
   [daisyproducer2.config :refer [env]]))

(def ^:private executable "/opt/dtbook2sbsform/dtbook2sbsform.sh")

(defn- log-process
  [proc]
  (log/info "Invoking" (:cmd proc)))

(defn- translate
  "Translate the given `dtbook` using given `args` and store it in
  `output-path`. If a `pipe` is also given use it to take the input of
  a pipe (typically from the hyphenation process)"
  ([dtbook output-path args]
   (let [opts {:out :write
               :out-file (fs/file output-path)
               :err :string
               :extra-env {"LANG" "en_US.UTF-8"}
               :pre-start-fn log-process}]
     (apply process/shell opts executable (format "-s:%s" dtbook) args)))
  ([pipe dtbook output-path args]
   (let [opts {:out :write
               :out-file (fs/file output-path)
               :err :string
               :extra-env {"LANG" "en_US.UTF-8"}
               :pre-start-fn log-process}]
     (apply process/shell pipe opts executable (format "-s:-") args))))

(defn- hyphenate
  [dtbook]
  (let [java (env :java17)]
    (process/shell
     {:out :string
      :extra-env {"LANG" "en_US.UTF-8"}
      :pre-start-fn log-process}
     java "-jar" "/usr/local/share/java/dtbook-hyphenator.jar" dtbook)))

(defn- hyphenate-and-translate
  [dtbook output-path opts]
  (-> (hyphenate dtbook)
      (translate dtbook output-path opts)))

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
        opts (map stringify-opts opts)]
    (if hyphenation?
      (hyphenate-and-translate dtbook output-path opts)
      (translate dtbook output-path opts))))
