(ns daisyproducer2.pipeline2.scripts
  "Thin layer above the [Pipeline2 Web Service
  API](https://daisy.github.io/pipeline/WebServiceAPI) to invoke
  specific scripts."
  (:require [clojure.string :as string]
            [clojure.java.io :as io]
            [daisyproducer2.pipeline2.core :as pipeline2]
            [clojure.set :as set]))

(defn validate [input & {:keys [mathml-version check-images] :as opts}]
  (pipeline2/create-job-and-wait "dtbook-validator" [input] [] (merge opts {:input-dtbook input})))

(defn dtbook-to-ebook [dtbook images epub]
  (pipeline2/with-job [job (pipeline2/job-create "sbs:dtbook-to-ebook" [dtbook] images {})]
    (let [completed (pipeline2/wait-for job)
          results (pipeline2/get-results completed)
          epub-stream (->> results
                           (filter #(string/ends-with? % ".epub"))
                           first
                           pipeline2/get-stream)]
      (with-open [in epub-stream
                  out (io/output-stream epub)]
        (io/copy in out)))))

(defn dtbook-to-odt [dtbook images odt
                     {:keys [asciimath phonetics image-handling line-numbers page-numbers page-numbers-float answer]
                      :or {asciimath "BOTH"
                           phonetics true
                           image-handling "LINK"
                           line-numbers true
                           page-numbers true
                           page-numbers-float false
                           answer "_.."}
                      :as opts}]
  (pipeline2/with-job [job (pipeline2/job-create "sbs:dtbook-to-odt" [dtbook] images (set/rename-keys opts {:image_handling :images}))]
    (let [completed (pipeline2/wait-for job)
          results (pipeline2/get-results completed)
          odt-stream (->> results
                          (filter #(string/ends-with? % ".odt"))
                          first
                          pipeline2/get-stream)]
      (with-open [in odt-stream
                  out (io/output-stream odt)]
        (io/copy in out)))))
