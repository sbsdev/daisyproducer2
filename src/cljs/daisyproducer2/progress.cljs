(ns daisyproducer2.progress
  (:require [re-frame.core :as rf]))

(rf/reg-sub
 ::progress
 (fn [db [_ id]]
   (-> db :progress id)))

(rf/reg-sub
 ::in-progress?
 (fn [[_ id]] (rf/subscribe [::progress id]))
 (fn [{:keys [value max] :as progress}] (and progress (< value max))))

(defn init-progress [db id max]
  (assoc-in db [:progress id] {:value 0 :max max}))

(defn update-progress [db id]
  (update-in db [:progress id :value] inc))

(defn progress-bar [id]
  (let [progress @(rf/subscribe [::progress id])
        {value :value max :max} progress]
    (when (and progress (< value max))
      [:<>
         [:label.label {:for (name id)} "Uploading Files"]
         [:progress.progress
          {:id (name id)
           :value value
           :max max}
          (str (* 100 (/ value max)) "%")]])))
