(ns daisyproducer2.documents.details
  (:require [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::synchronize
 (fn [{db :db} [_ id {:keys [path]}]]
   {:db (notifications/set-button-state db :document :synchronize)
    :http-xhrio
    (as-transit
     {:method :post
      :uri (str "/api/alfresco/" id)
      :headers 	     (auth/auth-header db)
      :on-success [::success]
      :on-failure [::failure]})}))

(rf/reg-event-db
 ::success
 (fn [db [_]]
   (notifications/clear-button-state db :document :synchronize)))

(rf/reg-event-db
 ::failure
 (fn [db [_ response]]
   (let [status-text (or (get-in response [:response :status-text])
                         (get response :status-text))
         errors (get-in response [:response :errors])]
     (-> db
         (notifications/set-errors :synchronize status-text errors)
         (notifications/clear-button-state :document :synchronize)))))

(defn synchronize-button
  [{id :id}]
  (let [loading? @(rf/subscribe [::notifications/button-loading? :document :synchronize])
        authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:button.button.is-success
     {:class (when loading? "is-loading")
      :disabled (not authenticated?)
      :on-click (fn [e] (rf/dispatch [::synchronize id]))}
     [:span.icon {:aria-hidden true} [:i.mi.mi-sync]]
     [:span (tr [:synchronize])]]))
