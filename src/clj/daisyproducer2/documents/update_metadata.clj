(ns daisyproducer2.documents.update-metadata
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set])
  (:import ch.sbs.MetaDataTransformer
           javax.xml.stream.XMLStreamException))

(def mapping
  {:author "dc:Creator"
   :date "dc:Date"
   :description "dc:Description"
   :identifier "dc:Identifier"
   :language "dc:Language"
   :production-series "prod:series"
   :production-series-number "prod:seriesNumber"
   :production-source "prod:source"
   :publisher "dc:Publisher"
   :rights "dc:Rights"
   :source "dc:Source"
   :source-date "dtb:sourceDate"
   :source-edition "dtb:sourceEdition"
   :source-publisher "dtb:sourcePublisher"
   :source-rights "dtb:sourceRights"
   :subject "dc:Subject"
   :title "dc:Title"})

(defn- rename-keys
  [{:keys [identifier] :as meta-data}]
  (-> meta-data
      (set/rename-keys mapping)
      ;; the identifier also needs to be put in the dtb:uid metadata field
      (cond-> identifier (assoc "dtb:uid" identifier))))

(defn- stringify-values
  [meta-data]
  (update-vals meta-data str))

(defn update-meta-data
  [old new meta-data]
  (let [in (io/input-stream old)
        out (io/output-stream new)
        meta-data (-> meta-data rename-keys stringify-values)]
    (try
      (MetaDataTransformer/transform in out meta-data)
      (catch XMLStreamException e
        (throw (ex-info "Failed to updte XML metadata"
                        {:error-id :failed-metadata-update}
                        e))))))
