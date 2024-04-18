(ns daisyproducer2.state-utils)

;; FIXME: This is majorly brittle and fails as soon as the ids in the
;; database change
(def keyword-to-state-id {:open 7 :closed 8})
(def state-id-to-keyword {7 :open 8 :closed})

