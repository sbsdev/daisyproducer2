(ns daisyproducer2.documents.state
  (:require
   [daisyproducer2.i18n :refer [tr]]))

;; FIXME: This is majorly brittle and fails as soon as the ids in the
;; database change
(def mapping {1 (tr [:new]) 7 (tr [:in-production]) 8 (tr [:finished])})
(def next-mapping {1 7 ; new -> in production
                   7 8 ; in production -> finished
                   8 1}) ; finished -> new

