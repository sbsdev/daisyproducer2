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
            [cheshire.core :as json])
  (:use [slingshot.slingshot :only [try+ throw+]]))

(defn- extract-paginated-result
  "Extract `count` and `id` from a paginated result as returned from the Alfresco REST API"
  [response]
  (let [{{{entries :entries
           {count :count} :pagination} :list} :body} response
        id (-> entries first :entry :id)]
    [id count]))

(defn- book
  "Return the id of the book node for a given `isbn`"
  [isbn]
  (let [{:keys [url user password]} (env :alfresco)
        query (format "select * from sbs:buch where sbs:pISBN = '%s' AND CONTAINS('PATH:\"/app:company_home/cm:Produktion/cm:Archiv//*\"')" isbn)
        query-body (json/generate-string {:query {:query query :language "cmis"}})]
    (let [[id count] (extract-paginated-result
                      (client/post (str url "/search/versions/1/search")
                                   {:as :json
                                    :basic-auth [user password]
                                    :body query-body}))]
      (if (= count 1)
        id
        (throw
         (ex-info (format "%s books in archive for ISBN '%s'" count isbn)
                  {:error-id :multiple-books-in-archive}))))))

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

(defn- content
  "Return the content for a given `node-id`"
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (-> (str url "/alfresco/versions/1/nodes/" node-id "/content")
        (client/get {:basic-auth [user password]})
        :body)))

(defn- content-stream
  "Return the content stream for a given `node-id`"
   [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (-> (str url "/alfresco/versions/1/nodes/" node-id "/content")
        (client/get {:as :stream :basic-auth [user password]})
        :body)))

(defn- images
  "Return a list of images for a given book `node-id`. The entries are
  in the form of a map `{:id :name}`"
  [node-id]
  (let [{:keys [url user password]} (env :alfresco)]
    (try+
      (-> (str url "/alfresco/versions/1/nodes/" node-id "/children")
          (client/get
           {:as :json
            :basic-auth [user password]
            :query-params {"relativePath" "Bilder"
                           "where" "(nodeType='sbs:graphic')"
                           ;; ignore the fact that this is paginated content. Just fetch
                           ;; lots of items so that we most likely get them all
                           "maxItems" 5000
                           "fields" "id,content,name"}})
          (get-in [:body :list :entries])
          (->>
           (filter (fn [item] (= (get-in item [:entry :content :mimeType]) "image/jpeg")))
           (map (fn [{entry :entry}] (select-keys entry [:id :name])))))
      (catch [:status 404] ;; return an empty list if there was a 404, i.e. there is no "Bilder" folder
          [])
      (catch Object _ (throw+)))))

(defn content-for-product [product-id]
  (let [daisy-file-node (-> product-id product parent daisy-file)]
    (content daisy-file-node)))

(defn content-for-isbn [isbn]
  (let [daisy-file-node (-> isbn book daisy-file)]
    (content daisy-file-node)))

(defn images-for-isbn [isbn]
  (let [book-node (book isbn)]
    (->> (images book-node)
         (map (fn [{:keys [id name]}] {:name name :content (content-stream id)})))))
