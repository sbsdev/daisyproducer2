(ns daisyproducer2.dtbook2sbsform
  "Wrapper around the dtbook2sbsform scripts.

  It is assumed that the dtbook2sbsform scripts have been installed
  under `/opt/dtbook2sbsform`."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as string]
   [clojure.tools.logging :as log]
   [clojure.java.io :as io]))

(def ^:private executable "/opt/dtbook2sbsform/dtbook2sbsform.sh")

(defn- hyphenator
  [dtbook]
  (process/shell "java" "-jar" "/home/eglic/src/dtbook_hyphenator/target/dtbook-hyphenator-1.2.2-shaded.jar" dtbook))

(defn- stringify-opts
  [[k v]]
  (let [opt-key (string/replace (name k) #"-" "_")]
    (cond
      (boolean? v) (format "?%s=%s" opt-key (if v "true()" "false()"))
      (integer? v) (format "?%s=%s" opt-key v)
      (keyword? v) (format "%s=%s" opt-key (name v))
      :else (format "%s=%s" opt-key v))))

(defn sbsform
  "Generate an SBSForm file for given `dtbook` into the file
  `output-path`. An exception is thrown if the document has no
  versions."
  ([dtbook output-path opts]
   (let [opts (map stringify-opts opts)
         input (format "-s:%s" dtbook)]
     (apply process/shell {:out :write :out-file (fs/file output-path)
                           :extra-env {"LANG" "en_US.UTF-8"}
                           :pre-start-fn #(log/info "Invoking" (:cmd %))}
            executable input opts))))
