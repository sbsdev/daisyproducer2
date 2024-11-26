(ns daisyproducer2.documents.products
  (:require [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [iapetos.collector.fn :as prometheus]))

(defn get-products
  ([document-id]
   (db/get-products {:document-id document-id}))
  ([document-id type]
   (db/get-products {:document-id document-id :type type})))

(defn get-product
  [id]
  (db/get-product {:id id}))

(defn insert-product
  [document-id product-number type]
  (->
   (db/insert-product {:product-number product-number :document-id document-id :type type})
   db/get-generated-key))

(defn delete-product
  "Delete a product given a product `id`. Return the number of rows affected."
  [id]
  (db/delete-product {:id id}))

(prometheus/instrument! metrics/registry #'get-products)
(prometheus/instrument! metrics/registry #'get-product)
(prometheus/instrument! metrics/registry #'insert-product)
(prometheus/instrument! metrics/registry #'delete-product)
