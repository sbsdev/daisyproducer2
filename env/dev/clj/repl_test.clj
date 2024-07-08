(ns repl-test
  (:require [java-time.api :as time]))

;; invoke the following expressions in a REPL inside the
;; daisyproducer2.documents.abacus namespace

(documents/get-document 49)

(versions/insert-initial-version (documents/get-document 49))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test the predicates ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(product-seen-before? "EB31506")
(source-or-title-source-edition-seen-before? {:source "" :title "" :source-edition ""})
(source-or-title-source-edition-seen-before? {:source "" :title "Übersetzungstest (MK)" :source-edition ""})
(source-or-title-source-edition-seen-before? {:source "" :title "Übersetzungstest (MK)" :source-edition "2. / 2006"})
(source-or-title-source-edition-seen-before? {:source "SBS362058" :title "" :source-edition ""})
(source-or-title-source-edition-seen-before? (documents/get-document 49))
(product-seen-before? "ET4728726")

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test update-document with unchanged metadata ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; ensure that the document has an associated product

(products/insert-product 49 "EB31506" 1)
(update-document (assoc (documents/get-document 49) :product-number "EB31506"))

;; make sure no new version was created

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test update-document with changed metadata ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(update-document (assoc (documents/get-document 49) :author "a test author" :product-number "EB31506"))

;; make sure the metadata is changed
;; select author from documents_document where id = 49;

;; make sure there is an new entry in documents_version
;; select * from documents_version where document_id = 49;

;; make sure a new version has been created in the file system with updated metadata
;; ~/tmp/49/versions/0GK6952K3MDXK.xml

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Try to change the production-series metadata ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(update-document (assoc (documents/get-document 49) :production-series "PPP" :product-number "EB31506"))

;; make sure the metadata is changed
;; select author from documents_document where id = 49;

;; make sure there is an new entry in documents_version
;; select * from documents_version where document_id = 49;

;; make sure a new version has been created in the file system with updated metadata
;; ~/tmp/49/versions/0GK6AKK67MFVC.xml

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Try to change the source metadata ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(update-document (assoc (documents/get-document 49) :source "SBS362058" :product-number "EB31506"))

;; make sure the metadata is changed
;; select author from documents_document where id = 49;

;; make sure there is an new entry in documents_version
;; select * from documents_version where document_id = 49;

;; make sure a new version has been created in the file system with updated metadata
;; ~/tmp/49/versions/0GK6FMNA7MC4D.xml

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test update-document-and-product ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(update-document-and-product (assoc (documents/get-document 49) :product-number "ET4728726"))

;; make sure the metadata is _not_ changed
;; select author from documents_document where id = 49;

;; make sure there is _no_ new entry in documents_version
;; select * from documents_version where document_id = 49;

;; make sure _no_ new version has been created in the file system

;; make sure the product has been added
;; select * from documents_product where document_id = 49;

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Test insert-document-and-product ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(insert-document-and-product
 {:production-series-number "6"
  :source-publisher ""
  :date (LocalDate/parse "2080-04-24")
 :production-series "PPP"
 :source-edition ""
 :publisher "SBS Schweizerische Bibliothek für Blinde, Seh- und Lesebehinderte"
 :product-number "PS86141"
 :source "33-508974-91-0"
 :title "Voilà"
 :author "Edgar Palmérise",
 :production-source "electronicData",
 :product-type :braille,
 :language "de",
 :daisyproducer? true})

;; make sure a new document has been created
;; select * from documents_document where title = "Voilà";
;; ✓

;; make sure there is an entry in documents_version
;; select * from documents_version where document_id = 761;
;; ✓

;; make sure there is an initial version in the file system
;; ✓

;; make sure the metadata of the initial version in the file system is correct
;; ✓

;; make sure the product has been added
;; select * from documents_product where document_id = 761;
;; ✓

