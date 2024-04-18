(ns daisyproducer2.utils)

(defn is-admin? [{:keys [roles] :as user}]
  (contains? roles "edit_global_words"))
