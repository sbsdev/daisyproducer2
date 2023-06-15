(ns daisyproducer2.pipeline2.scripts
  "Thin layer above the [Pipeline2 Web Service
  API](https://daisy.github.io/pipeline/WebServiceAPI) to invoke
  specific scripts."
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [daisyproducer2.pipeline2.core :as pipeline2]))

(defn validate [input & {:keys [mathml-version check-images] :as opts}]
  (pipeline2/create-job-and-wait "dtbook-validator" {} (merge opts {:input-dtbook input})))

(defn daisy3-to-epub3 [input & {:keys [mediaoverlays assert-valid] :as opts}]
  (pipeline2/create-job-and-wait "daisy3-to-epub3" {:source input} opts))

(defn epub3-to-daisy202 [input & {:keys [temp-dir output-dir] :as opts}]
  (pipeline2/create-job-and-wait "epub3-to-daisy202" {} (merge {:epub input} opts)))

(defn dtbook-to-ebbook [dtbook epub]
  (pipeline2/with-job [job (pipeline2/job-create "sbs:dtbook-to-ebook" {:source dtbook} {})]
    (let [completed (pipeline2/wait-for-result job)
          results (pipeline2/get-results completed)
          epub-stream (->> results
                           (filter #(string/ends-with? % ".epub"))
                           first
                           pipeline2/get-stream)]
      (with-open [in epub-stream
                  out (io/output-stream epub)]
        (io/copy in out)))))


