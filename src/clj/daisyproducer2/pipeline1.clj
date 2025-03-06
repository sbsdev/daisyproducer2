(ns daisyproducer2.pipeline1
  "Wrapper around the DAISY pipeline 1 scripts.

  It is assumed that there is a binary named `daisy-pipeline` on the
  path and that the pipeline scripts are installed under
  `/usr/lib/daisy-pipeline/scripts`."
  (:require
   [babashka.fs :as fs]
   [babashka.process :as process]
   [clojure.set :as set]
   [clojure.string :as str]
   [clojure.tools.logging :as log]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.documents.utils :refer [with-tempdir]]
   [medley.core :refer [update-existing]]))

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

(def ^:private key-mapping
  "Parameter name mapping between Clojure and the Pipeline1 scripts"
  {:font-size :fontsize
   :backup-font :backupFont
   :backup-unicode-ranges :backupUnicodeRanges
   :page-style :pageStyle
   :stock-size :stocksize
   :line-spacing :line_spacing
   :left-margin :left_margin
   :right-margin :right_margin
   :top-margin :top_margin
   :bottom-margin :bottom_margin
   :replace-em-with-quote :replace_em_with_quote
   :end-notes :endnotes
   :image-visibility :image_visibility})

(defn- to-pipeline1
  "Convert parameter values from Clojure keywords to strings that the Pipeline1 script understands"
  [opts]
  (-> opts
      ;; the font-size is expected to be a string in the form of '17pt'
      (update-existing :font-size #(format "%spt" %))
      (update-existing :font #(case % :tiresias "Tiresias LPfont" :roman "Latin Modern Roman"
                                    :sans "Latin Modern Sans" :mono "Latin Modern Mono"))
      (update-existing :page-style #(case % :plain "plain" :compact "compact" :with-page-nums "withPageNums"
                                          :spacious "spacious" :scientific "scientific" ))
      (update-existing :stock-size #(case % :a3paper "a3paper" :a4paper "a4paper"))
      (update-existing :line-spacing #(case % :singlespacing "singlespacing" :onehalfspacing "onehalfspacing"
                                            :doublespacing "doublespacing"))
      (update-existing :paperwidth #(format "%smm" %))
      (update-existing :paperheight #(format "%smm" %))
      (update-existing :left-margin #(format "%smm" %))
      (update-existing :right-margin #(format "%smm" %))
      (update-existing :top-margin #(format "%smm" %))
      (update-existing :bottom-margin #(format "%smm" %))
      (update-existing :alignment #(case % :left "left" :justified "justified"))
      (update-existing :end-notes #(case % :none "none" :document "document" :chapter "chapter"))
      (update-existing :image-visibility #(case % :show "show" :ignore "ignore"))))

(defn dtbook-to-latex
  "Invoke the LaTeX conversion script. See the Pipeline documentation for all possible options."
  [input output opts]
  (let [args (-> opts
                 ;; convert parameter values
                 to-pipeline1
                 ;; map the keys to the ones that the pipeline1 expects
                 (set/rename-keys key-mapping)
                 (merge {:input input :output output}))
        script (str (fs/path (env :pipeline1-install-path)
                             "scripts/create_distribute/latex/DTBookToLaTeX.taskScript"))]
    (try
      (apply process/shell
             {:err :string
              :out :string
              :pre-start-fn #(log/info "Invoking" (:cmd %))}
             "daisy-pipeline" script (map (fn [[k v]] (format "--%s=%s" (name k) v)) args))
      (catch clojure.lang.ExceptionInfo e
        (log/error e)
        (throw
         (ex-info (format "Conversion of %s failed" input) {:error-id ::latex-conversion-failed} e))))))

(defn latex-to-pdf
  "Invoke `latexmk` on the given `input` file and store the resulting
  PDF in the given `output` file."
  [input images output]
  (with-tempdir [tempdir {:prefix "daisyproducer-"}]
    (let [pdf-path (fs/path tempdir (str (fs/file-name (fs/strip-ext input)) ".pdf"))]
      (try
        ;; copy the latex file to the tempdir
        (fs/copy input tempdir)
        ;; copy the images into the temp dir so that LaTeX can find them
        (doseq [image images] (fs/copy image tempdir))
        (process/shell {:err :string
                        :out :string
                        :dir tempdir
                        :pre-start-fn #(log/info "Invoking" (:cmd %))}
                       "latexmk" "-interaction=batchmode" "-xelatex" input)
        (fs/move pdf-path output {:replace-existing true})
        (catch clojure.lang.ExceptionInfo e
          (log/error (ex-message e))
          ;; move the failing tex files away for later inspection
          (fs/move tempdir (fs/create-dirs "/tmp/daisyproducer2-failed"))
          (throw
           (ex-info (format "Conversion of %s failed" input) {:error-id ::pdf-conversion-failed} e)))
        (catch java.nio.file.NoSuchFileException e
          (log/error (ex-message e))
          (throw
           (ex-info (format "Move of %s to %s failed" pdf-path output) {:error-id ::no-such-file} e)))))))

(defn insert-volume-split-points
  "Invoke the script to insert volume split points on the given `input`
  file and store the resulting XML in the given `output` file."
  [input output volumes]
  (let [script (str (fs/path (env :pipeline1-install-path)
                             "scripts/modify_improve/dtbook/DTBookVolumeSplit.taskScript"))
        args {:input input :output output :number_of_volumes volumes}]
    (try
      (apply process/shell
       {:err :string
        :out :string
        :pre-start-fn #(log/info "Invoking" (:cmd %))}
       "daisy-pipeline" script (map (fn [[k v]] (format "--%s=%s" (name k) v)) args))
      (catch clojure.lang.ExceptionInfo e
        (log/error e)
        (throw
         (ex-info (format "Insertion of volume split points for %s failed" input)
                  {:error-id ::volume-split-points-insertion-failed} e))))))
