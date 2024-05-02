(ns daisyproducer2.routes.static
  (:require
   [daisyproducer2.config :refer [env]]
   [reitit.ring :as ring]))

(defn archive-routes
  "Route to serve static assets such as DTBook XML of versions and images directly from the file system."
  []
  ["/archive/*"
   (ring/create-file-handler
    {:root (env :document-root)})])

(defn created-assets-routes
  "Route to serve generated assets such as EPUBs, SBSform, PDFs or ODT files directly from the file system."
  []
  ["/download/*"
   (ring/create-file-handler
    {:root (env :spool-dir)})])
