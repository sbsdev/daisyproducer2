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
  {:asciimath :both
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
  (let [opts (merge odt-defaults opts)
        opts (-> opts
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
