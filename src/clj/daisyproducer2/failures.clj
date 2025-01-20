(ns daisyproducer2.failures
  (:require
   [failjure.core :as f]))

(defrecord AnnotatedFailure [message errors]
  f/HasFailed
  (failed? [self] true)
  (message [self] (:message self)))

(defn annotated-fail [msg errors]
  (->AnnotatedFailure msg errors))
