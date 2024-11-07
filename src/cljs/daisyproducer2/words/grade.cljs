(ns daisyproducer2.words.grade
  (:require
   [re-frame.core :as rf]
   [daisyproducer2.i18n :refer [tr]]))

; FIXME: Instead of a default value, maybe there should be an init
; event that initializes the value of current-grade at the start of
; the app
(defn get-grade [db] (get db :current-grade 0))

(rf/reg-event-fx
  ::set-grade
  (fn [{:keys [db]} [_ grade dispatch]]
    (let [id (-> db :current :document :id)]
      {:db (assoc db :current-grade (js/parseInt grade))
       :dispatch [dispatch id]})))

(rf/reg-sub ::grade (fn [db _] (get-grade db)))

(defn selector [dispatch-event]
  (let [current @(rf/subscribe [::grade])
        getvalue (fn [e] (-> e .-target .-value))
        emit     (fn [e] (rf/dispatch [::set-grade (getvalue e) dispatch-event]))]
    [:div.field
     [:div.control
      [:div.select.is-fullwidth
       [:select
        {:value current
         :on-change emit}
        (for [[v s] [[1 (tr [:uncontracted])]
                     [2 (tr [:contracted])]
                     [0 (tr [:both-grades])]]]
          ^{:key v}
          [:option {:value v} s])]]]]))
