(ns daisyproducer2.routes.static
  (:require
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.documents.documents :as documents]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring :as ring]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.parameters :as parameters]
   [ring.util.http-response :as response]))

(defn archive-routes
  "Route to serve static assets such as DTBook XML of versions and images directly from the file system."
  []
  ["/archive/*"
   (ring/create-file-handler
    {:root (env :document-root)})])

(defn- content-response [file name]
  (let [content-disposition (format "attachment; filename=\"%s\"" name)]
    (-> file
        (response/file-response {:root (env :spool-dir)})
        (response/header "Content-Disposition" content-disposition))))

(defn- created-assets-handler [name-fn]
  (fn [{{{:keys [id file] :as params} :path} :parameters}]
    (if-let [doc (documents/get-document id)]
      (let [title (:title doc)
            name (name-fn title params)]
        (content-response file name))
      (response/not-found))))

(defn created-assets-routes
  "Route to serve generated assets such as EPUBs, SBSform, PDFs or ODT files."
  []
  ["/download/:id"
   {:coercion spec-coercion/coercion
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware]}
   ["/epub/:file" {:parameters {:path {:id int?
                                       :file string?}}
                   :handler (created-assets-handler
                             (fn [title _] (format "%s.epub" title)))}]

   ["/braille/:contraction/:file" {:parameters {:path {:id int?
                                                       :contraction int?
                                                       :file string?}}
                                   :handler (created-assets-handler
                                             (fn [title {:keys [contraction]}]
                                               (format "%s.%s" title (if (= contraction 2) "bk" "bv"))))}]

   ["/large-print/:font-size/:file" {:parameters {:path {:id int?
                                                         :font-size int?
                                                         :file string?}}
                                     :handler (created-assets-handler
                                             (fn [title {:keys [font-size]}]
                                               (format "%s %spt.pdf" title font-size)))}]

   ["/open-document/:file" {:parameters {:path {:id int?
                                                :file string?}}
                            :handler (created-assets-handler
                                      (fn [title _] (format "%s.odt" title)))}]
   ["/html/:file" {:parameters {:path {:id int?
                                       :file string?}}
                   :handler (created-assets-handler
                             (fn [title _] (format "%s.html" title)))}]])

