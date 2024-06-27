(ns daisyproducer2.documents.update-metadata
  (:require
   [clojure.java.io :as io]
   [clojure.set :as set])
  (:import ch.sbs.MetaDataTransformer))

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

(defn- rename-meta-data-keys
  [{:keys [identifier] :as meta-data}]
  (-> meta-data
      (set/rename-keys mapping)
      ;; the identifier also needs to be put in the dtb:uid metadata field
      (cond-> identifier (assoc "dtb:uid" identifier))))

(defn update-meta-data
  [old new meta-data]
  (let [in (io/input-stream old)
        out (io/output-stream new)
        meta-data (rename-meta-data-keys meta-data)]
    (MetaDataTransformer/transform in out meta-data)))
