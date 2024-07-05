(ns daisyproducer2.test.abacus-import.db-test
  (:require
   [babashka.fs :as fs]
   [clojure.data :as data]
   [clojure.java.io :as io]
   [clojure.test :refer :all]
   [clojure.xml :as xml]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.db.core :as db :refer [*db*]]
   [daisyproducer2.documents.abacus :as abacus]
   [mount.core :as mount]
   [daisyproducer2.documents.products :as products]))

;; These tests are extremely brittle and depend on a lot of things
;; existing in the database (documents, associated versions,
;; associated products) and in the file system (versions in the
;; document-root directory)

;; Take the tests more like written down recipes for manual testing

(defrecord Document [id title author subject description publisher date
                     identifier source language rights
                     source-date source-edition source-publisher source-rights
                     production-series production-series-number production-source])

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'daisyproducer2.config/env
     #'daisyproducer2.db.core/*db*)
    #_(migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest ^:database test-update-same-document
  (let [doc (db/get-document {:id 49})
        product-number "EB31506"
        latest-version (db/get-latest-version {:document-id 49})
        ;; make sure the product exists for the document
        product-id (products/insert-product 49 product-number 1)]
    (abacus/update-document doc product-number)
    ;; make sure the version was not updated as the document hasn't changed
    (is (= latest-version (db/get-latest-version {:document-id 49})))
    ;; undo the changes
    (db/delete-product {:id product-id})))

(deftest ^:database test-update-document
  (let [doc (db/get-document {:id 49})
        new (assoc doc :author "a test author")
        product-number "EB31506"
        latest-version (db/get-latest-version {:document-id 49})
        ;; make sure the product exists for the document
        product-id (products/insert-product 49 product-number 1)]
    ;; make sure version exists in the file system
    (fs/copy-tree (fs/file (io/resource "49")) (fs/path (env :document-root) "49")
                  {:replace-existing true})
    ;; update the document
    (abacus/update-document new product-number)
    ;; make sure the version was updated as the document changed
    (is (not= latest-version (db/get-latest-version {:document-id 49})))
    ;; the only difference in the XML is the changed author
    (is (= {:content [{:content [nil {:attrs {:content "a test author"}}]}]}
           (second (data/diff (xml/parse (fs/file (fs/path (env :document-root)
                                                           (:content latest-version))))
                              (xml/parse (fs/file (fs/path (env :document-root)
                                                           (:content (db/get-latest-version {:document-id 49})))))))))
    ;; undo the changes
    (db/update-document-meta-data doc)
    (db/delete-product {:id product-id})
    (db/delete-version (db/get-latest-version {:document-id 49}))
    (fs/delete-tree (fs/path (env :document-root) "49"))))


