(ns daisyproducer2.pipeline1
  "Wrapper around the DAISY pipeline 1 scripts.

  It is assumed that there is a binary named `daisy-pipeline` on the
  path and that the pipeline scripts are installed under
  `/usr/lib/daisy-pipeline/scripts`."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [daisyproducer2.config :refer [env]]))

(defn continuation-line? [line]
  (cond
   (re-find #"^\s+" line) true ; starts with white space
   (re-find #".*:$" line) true ; ends in a colon
   (= "" line) true
   :else line))

(defn- clean-line [line file]
  (-> line
      (str/replace "[ERROR, Validator]" "")
      (str/replace (str "Location: file:" file) "Line:")
      str/trim))

(defn- filter-output [output file]
  (->> output
       str/split-lines
       ;; make sure we merge continuation lines with their log line
       (partition-by continuation-line?)
       (map str/join)
       (filter #(re-matches #"^\[ERROR, Validator\].*" %))
       (map #(clean-line % file))))

(defn validate
  "Invoke the validator script of `type` for given `file`. Returns an
  empty seq on successful validation or a seq of error messages
  otherwise. Possible types are `:dtbook` or `:daisy202`"
  [file type]
  ;; the pipeline1 validator is not so brilliant when it comes to
  ;; returning error conditions. For that reason we check for
  ;; existence of the input file before hand
  (if-not (and (fs/exists? file) (fs/readable? file))
    [(format "Input file '%s' does not exist or is not readable" file)]
    (let [args ["daisy-pipeline"
                (str (fs/path (env :pipeline1-install-path)
                              "scripts/verify/ConfigurableValidator.taskScript"))
                (str "--validatorInputFile=" file)]
          dtbook [;; make sure it is a DTBook file
                  "--validatorRequireInputType=Dtbook document"
                  ;; make sure files with a missing DOCTYPE declaration do not validate
                  "--validatorInputDelegates=org.daisy.util.fileset.validation.delegate.impl.NoDocTypeDeclarationDelegate"]
          daisy202 [;; make sure it is a DAISY 202 file
                    "--validatorRequireInputType=DAISY 2.02 DTB"]]
      (-> (apply process/sh (concat args (case type :dtbook dtbook :daisy202 daisy202)))
          :out
          (filter-output file)))))

(defn audio-encoder
  "Invoke the audio encoder script."
  [input output & {:keys [bitrate stereo freq] :as opts}]
  (let [args (merge opts {:input input :output output})
        script (fs/path (env :pipeline1-install-path)
                        "scripts/modify_improve/dtb/DTBAudioEncoder.taskScript")]
    (try
      (apply process/shell "daisy-pipeline" (str script)
             (map (fn [[k v]] (format "--%s=%s" (name k) v)) args))
      (catch clojure.lang.ExceptionInfo e
        (log/error (ex-message e))
        (throw
         (ex-info (format "Audio encoding of %s failed" input) {:error-id ::audio-encoding-failed}))))))
