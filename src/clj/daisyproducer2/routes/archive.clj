(ns daisyproducer2.routes.archive
  (:require
   [daisyproducer2.config :refer [env]]
   [reitit.ring :as ring]))

(defn archive-routes
  "Route to serve static assets such as DTBook XML of versions and images directly from the file system."
  []
  ["/archive/*"
   (ring/create-file-handler
    {:root (env :document-root)})])

