(ns daisyproducer2.pipeline2.scripts
  "Thin layer above the [Pipeline2 Web Service
  API](https://daisy.github.io/pipeline/WebServiceAPI) to invoke
  specific scripts."
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set]
   [clojure.string :as str]
   [daisyproducer2.pipeline2.core :as pipeline2]
   [medley.core :refer [update-existing]]))

(defn validate [input & {:keys [mathml-version check-images] :as opts}]
  (pipeline2/create-job-and-wait "dtbook-validator" [input] [] (merge opts {:input-dtbook input})))

(defn dtbook-to-ebook [dtbook images epub]
  (pipeline2/with-job [job (pipeline2/job-create "sbs:dtbook-to-ebook" [dtbook] images {})]
    (let [completed (pipeline2/wait-for job)
          results (pipeline2/get-results completed)
          epub-stream (->> results
                           (filter #(str/ends-with? % ".epub"))
                           first
                           pipeline2/get-stream)]
      (with-open [in epub-stream
                  out (io/output-stream epub)]
        (io/copy in out)))))

(defn dtbook-to-html [dtbook images html]
  (pipeline2/with-job [job (pipeline2/job-create "dtbook-to-html" [dtbook] images {})]
    (let [completed (pipeline2/wait-for job)
          results (pipeline2/get-results completed)
          _ (println results)
          epub-stream (->> results
                           (filter #(str/ends-with? % ".xhtml"))
                           first
                           pipeline2/get-stream)]
      (with-open [in epub-stream
                  out (io/output-stream html)]
        (io/copy in out)))))

(def ^:private odt-defaults
  {:math :both
   :phonetics true
   :image-handling :link
   :line-numbers true
   :page-numbers true
   :page-numbers-float false
   :answer "_.."})

(def ^:private key-mapping
  "Parameter name mapping between Clojure and the Pipeline2 scripts"
  {:image-handling :images
   :math :asciimath})

(defn- to-pipeline2
  "Convert parameter values from Clojure keywords to strings that the Pipeline2 script understands"
  [opts]
  (-> opts
      (update-existing :math #(case % :asciimath "ASCIIMATH" :mathml "MATHML" :both "BOTH"))
      (update-existing :image-handling #(case % :embed "EMBED" :link "LINK" :drop "DROP"))))

(defn dtbook-to-odt
  "Invoke the Pipeline2 script to create an ODT file `odt` given the
  `input`, the `images` and the options given in `opts`"
  [dtbook images odt opts]
  (let [opts (-> opts
                 (->> (merge odt-defaults))
                 to-pipeline2
                 (set/rename-keys key-mapping))]
    (pipeline2/with-job [job (pipeline2/job-create "sbs:dtbook-to-odt" [dtbook] images opts)]
      (let [completed (pipeline2/wait-for job)
            results (pipeline2/get-results completed)
            odt-stream (->> results
                            (filter #(str/ends-with? % ".odt"))
                            first
                            pipeline2/get-stream)]
        (with-open [in odt-stream
                    out (io/output-stream odt)]
          (io/copy in out))))))

(def ^:private ebraille-defaults
  {:epub-package true
   :validation "off"
   :include-original-text true})

(defn dtbook-to-ebraille
  "Invoke the Pipeline2 script to create an eBraille file `ebraille`
  given the `input`, the `images` and the options given in `opts`"
  [dtbook images ebraille opts]
  (let [opts (-> opts
                 (->> (merge ebraille-defaults))
                 to-pipeline2
                 (set/rename-keys key-mapping))]
    (pipeline2/with-job [job (pipeline2/job-create "dtbook-to-ebraille" [dtbook] images opts)]
      (let [completed (pipeline2/wait-for job)
            results (pipeline2/get-results completed)
            stream (->> results
                        (filter #(str/ends-with? % ".ebrl"))
                        first
                            pipeline2/get-stream)]
        (with-open [in stream
                    out (io/output-stream ebraille)]
          (io/copy in out))))))

(def ^:private latex-key-mapping
  "Parameter name mapping between Clojure and the dtbook-to-latex script"
  {:paperwidth :paper-width
   :paperheight :paper-height
   :end-notes :endnotes
   :image-visibility :include-images})

(defn- to-latex
  "Convert parameter values from Clojure keywords to strings that the dtbook-to-latex script understands"
  [opts]
  (-> opts
      (update-existing :font-size #(format "%spt" %))
      (update-existing :font #(case % :tiresias "Tiresias LPfont" :roman "Latin Modern Roman"
                                    :sans "Latin Modern Sans" :mono "Latin Modern Mono"))
      (update-existing :page-style #(case % :plain "plain" :compact "compact" :with-page-nums "withPageNums"
                                          :spacious "spacious" :scientific "scientific"))
      (update-existing :stock-size name)
      (update-existing :line-spacing name)
      (update-existing :alignment name)
      (update-existing :end-notes name)
      (update-existing :paperwidth #(format "%smm" %))
      (update-existing :paperheight #(format "%smm" %))
      (update-existing :left-margin #(format "%smm" %))
      (update-existing :right-margin #(format "%smm" %))
      (update-existing :top-margin #(format "%smm" %))
      (update-existing :bottom-margin #(format "%smm" %))
      ;; the conversion asks whether to include the images, not whether to ignore them
      (update-existing :image-visibility #(= % :show))))

(defn dtbook-to-latex
  "Invoke the Pipeline2 script to create a LaTeX file `latex` given the
  `dtbook` and the options given in `opts`. The images are not needed:
  the conversion only refers to them, it does not read them."
  [dtbook latex opts]
  (let [opts (-> opts to-latex (set/rename-keys latex-key-mapping))]
    (pipeline2/with-job [job (pipeline2/job-create "dtbook-to-latex" [dtbook] [] opts)]
      (let [completed (pipeline2/wait-for job)
            results (pipeline2/get-results completed)
            latex-stream (->> results
                              (filter #(str/ends-with? % ".tex"))
                              first
                              pipeline2/get-stream)]
        (with-open [in latex-stream
                    out (io/output-stream latex)]
          (io/copy in out))))))
