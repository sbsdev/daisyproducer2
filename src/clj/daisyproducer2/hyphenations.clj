(ns daisyproducer2.hyphenations
  (:require [clojure.tools.logging :as log]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.whitelists.hyphenation :as hyphenations]
            [iapetos.collector.fn :as prometheus]))

(defn get-hyphenations [spelling search limit offset]
  (db/get-hyphenation {:spelling spelling :search (db/search-to-sql search)
                       :limit limit :offset offset}))

(defn put-hyphenation [word hyphenation spelling]
  (log/debug "Add hyphenation" word)
  (db/insert-hyphenation {:word word :hyphenation hyphenation :spelling spelling})
  (hyphenations/export))

(defn delete-hyphenation [word spelling]
  (log/debug "Delete hyphenation" word)
  (let [deleted (db/delete-hyphenation {:word word :spelling spelling})]
    (when (> deleted 0)
      (hyphenations/export))
    deleted))

(prometheus/instrument! metrics/registry #'get-hyphenations)
(prometheus/instrument! metrics/registry #'put-hyphenation)
(prometheus/instrument! metrics/registry #'delete-hyphenation)
