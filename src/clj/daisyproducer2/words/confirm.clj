(ns daisyproducer2.words.confirm
  (:require [conman.core :as conman]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.whitelists.hyphenation :as hyphenations]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.global :as global]
            [daisyproducer2.words.local :as local]
            [iapetos.collector.fn :as prometheus]))

(defn get-words [limit offset]
  (->> (db/get-confirmable-words {:limit limit :offset offset})
       (map words/int-fields-to-boolean)
       (map words/complement-braille)
       (map words/complement-ellipsis-braille)
       (map words/complement-hyphenation)))

(defn put-word
  "Confirm the (local) `word`. If it is marked as `:islocal` then it
  is kept in the local words and also marked as `:isconfirmed`.
  Otherwise if is actually contained in the local words it is deleted
  from there and added to the global words and the number of additions
  are returned. If it is not contained in the local words 0 is
  returned."
  [word]
  ;; in confirm you do not typically update the hyphenation but it can happen
  (when (:hyphenated word)
    (db/insert-hyphenation
     (words/to-db word words/hyphenation-keys words/hyphenation-mapping))
    (hyphenations/export))
  (if (:islocal word)
    ;; if a word is local then just save it in the local db with
    ;; confirmed = true
    (-> word (assoc :isconfirmed true) (local/put-word))
    ;; otherwise move the word to the global dict
    (conman/with-transaction [db/*db*]
      ;; drop the hyphenation, otherwise the hyphenation is removed
      ;; and right after added again
      (let [word (dissoc word :hyphenated)
            ;; in some cases the type of the word is changed at the
            ;; same time as it is confirmed. In that case we need to
            ;; do the deletion using the old type, otherwise we will
            ;; not find it.
            type (:type word)
            old-type (case (int type)
                       1 2 ; if :type-name-hoffmann it was previously :type-name
                       3 4 ; if :type-place-langenthal it was previously :type-place
                       type)
            deletions (local/delete-word (assoc word :type old-type))]
        (if-not (> deletions 0)
          ;; if we couldn't delete anything then presumably this word
          ;; doesn't exist in the local words, so there is certainly
          ;; no point in putting it in the global words. Just return 0
          ;; as the number of modifications.
          deletions
          ;; if the word was in the local words we managed to delete
          ;; it and we can savely add it to the global words. Return
          ;; the number of additions.
          (global/put-word word))))))

(prometheus/instrument! metrics/registry #'get-words)
(prometheus/instrument! metrics/registry #'put-word)
