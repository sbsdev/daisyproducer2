(ns daisyproducer2.words.totals
  (:require [re-frame.core :as rf]))

;; to avoid circular dependencies we extract some of the functionality
;; for totals to a separate namespace.
;;
;; Namely the unknown namespace needs to increment the totals in the
;; local namespace when saving an unknown word. Likewise the local
;; namespace needs to increment the totals in the unknown namespace
;; when deleting a local word.

(rf/reg-event-db
 ::increment-unknown-words
 (fn [db [_]] (update-in db [:totals :unknown] inc)))

(rf/reg-event-db
 ::increment-local-words
 (fn [db [_]] (update-in db [:totals :local] inc)))

