(ns daisyproducer2.documents.alfresco
  "Synchronize files with an Alfresco archive.

  Productions are created from XML files and images. During the
  production process these are kept on local disk, but once the
  production is finished they are stored in an Alfreco archive.

  This namespace provides functionality to fetch content from the
  archive using the [Alfresco ReST
  API](https://docs.alfresco.com/content-services/latest/develop/rest-api-guide/)."

  (:require [clj-http.client :as client]
            [daisyproducer2.config :refer [env]]
            [cheshire.core :as json]))

(defn- extract-paginated-result
  "Extract `count` and `id` from a paginated result as returned from the Alfresco REST API"
  [response]
  (let [{{{entries :entries
           {count :count} :pagination} :list} :body} response
        id (-> entries first :entry :id)]
    [id count]))

(defn- product
  "Return the id of the product node for a given `product-id`"
  [product-id]
  (let [{:keys [url user password]} (env :alfresco)
        query (format "select * from sbs:produkt where sbs:pProduktNo = '%s' AND CONTAINS('PATH:\"/app:company_home/cm:Produktion/cm:Archiv//*\"')" product-id)
        query-body (json/generate-string {:query {:query query :language "cmis"}})]
    (let [[id count] (extract-paginated-result
                      (client/post (str url "/search/versions/1/search")
                                   {:as :json
                                    :basic-auth [user password]
                                    :body query-body}))]
      (if (= count 1)
        id
        (throw
         (ex-info (format "more than one product in archive for product '%s'" product-id)
                  {:error-id :multiple-products-in-archive}))))))

(defn- parent
  "Return the id of the parent node for a given `node-id`"
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (let [[id count] (extract-paginated-result
                      (client/get (str url "/alfresco/versions/1/nodes/" node-id "/parents")
                                  {:as :json
                                   :basic-auth [user password]}))]
      (if (= count 1)
        id
        (throw
         (ex-info (format "more than one parent in archive for node '%s'" node-id)
                  {:error-id :multiple-parents-in-archive}))))))

(defn- daisy-file
  "Return the id of the daisyFile node for a given `node-id`. Typically
  the given `node-id` refers to a node with type 'sbs:buch'."
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (let [[id count] (extract-paginated-result
                      (client/get (str url "/alfresco/versions/1/nodes/" node-id "/children")
                                  {:as :json
                                   :basic-auth [user password]
                                   :query-params {"where" "(nodeType='sbs:daisyFile')"}}))]
      (if (= count 1)
        id
        (throw
         (ex-info (format "more than one daisy-file in archive for node '%s'" node-id)
                  {:error-id :multiple-parents-in-archive}))))))

(defn- latest-version
  "Return the version id of the latest version for a given `node-id`"
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (first (extract-paginated-result
            (client/get (str url "/alfresco/versions/1/nodes/" node-id "/versions")
                        {:as :json
                         :basic-auth [user password]
                         :query-params {"maxItems" "1"}})))))

(defn- content
  "Return the content for a given `node-id` and `version-id`"
  [node-id version-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (:body (client/get (str url "/alfresco/versions/1/nodes/" node-id "/versions/" version-id "/content")
                      {:basic-auth [user password]}))))

(defn- images
  "Return a list of image node-ids for a given book `node-id`"
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (-> (str url "/alfresco/versions/1/nodes/" node-id "/children")
        (client/get
         {:as :json
          :basic-auth [user password]
          :query-params {"relativePath" "Bilder"
                         "where" "(nodeType='sbs:graphic')"
                         ;; ignore the fact that this is paginated content. Just fetch
                         ;; lots of items so that we most likely get them all
                         "maxItems" 5000
                         "fields" "id,content"}})
        (get-in [:body :list :entries])
        (->>
         (filter (fn [item] (= (get-in item [:entry :content :mimeType]) "image/jpeg")))
         (map #(get-in % [:entry :id]))))))

(defn- image-content [ids]
  (let [{:keys [url user password]} (env :alfresco)]
    (client/post (str url "/alfresco/versions/1/downloads/")
                 {:as :json
                  :basic-auth [user password]
                  :body (json/generate-string {:nodeIds (apply vector ids)})})
    ;; grab the id of the download from the response
    ;; wait until the download is ready, i.e. the response contains :status "DONE"
    ;; then fetch the content
    ;; all of this probably asynchronously
    ))

(defn content-for-product [product-id]
  (let [daisy-file-node (-> product-id product parent daisy-file)
        version (latest-version daisy-file-node)]
    (content daisy-file-node version)))
