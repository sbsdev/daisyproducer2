(ns dp2.words.notifications
  (:require [re-frame.core :as rf]
            [dp2.i18n :refer [tr]]))

(rf/reg-sub
  ::loading?
  (fn [db [_ id]]
    (-> db :loading id)))

(rf/reg-sub
  ::button-loading?
  (fn [db [_ page id which]]
    (-> db :loading :buttons page (get id) which)))

(rf/reg-event-db
 ::ack-error
 (fn [db [_ error-id]]
   (update db :errors dissoc error-id)))

(rf/reg-sub
 ::errors
 (fn [db _]
   (-> db :errors)))

(rf/reg-sub
 ::errors?
 :<- [::errors]
 (fn [errors] (seq errors)))

(defn set-button-state [db page id which]
  (assoc-in db [:loading :buttons page id which] true))

(defn clear-button-state [db page id which]
  (update-in db [:loading :buttons page id] dissoc which))

(defn loading-spinner []
  [:div.block
   [:p.has-text-centered.has-text-weight-semibold (tr [:loading])]
   [:button.button.is-large.is-fullwidth.is-loading (tr [:loading])]])

(defn error-notification []
  (let [errors @(rf/subscribe [::errors])]
    (when errors
      [:div.block
       (for [[k v] errors]
         ^{:key k}
         [:div.notification.is-danger
          [:button.delete
           {:on-click (fn [e] (rf/dispatch [::ack-error k]))}]
          [:strong (str k ":")] (str " " v)])]))) ; FIXME: Translation?
