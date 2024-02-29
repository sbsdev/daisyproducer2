(ns daisyproducer2.documents.state
  (:require
   [daisyproducer2.i18n :refer [tr]]))

;; FIXME: This is majorly brittle and fails as soon as the ids in the
;; database change
(def mapping {1 (tr [:new]) 7 (tr [:in-production]) 8 (tr [:finished])})
(def next-mapping {1 7 ; new -> in production
                   7 8 ; in production -> finished
                   8 1}) ; finished -> new

;; define a bulma tag class for each state, see https://bulma.io/documentation/elements/tag/
(def klass {1 "is-light" 7 "is-success" 8 "is-dark"})

(defn state
  "Component to diplay the state in a Bulma tag"
  [state-id]
  (let [state (mapping state-id state-id)
        klass (klass state-id)]
    [:span.tag {:class klass} state]))
