(ns daisyproducer2.documents.markup
  (:require [ajax.core :as ajax]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-latest-version
  (fn [{:keys [db]} [_ document-id]]
    {:db (assoc-in db [:loading :markup] true)
     :http-xhrio (as-transit
                  {:method          :get
                   :uri             (str "/api/documents/" document-id "/versions")
                   :params          {:latest true}
                   :on-success      [::fetch-markup document-id]
                   :on-failure      [::fetch-failure]})}))

(rf/reg-event-fx
 ::fetch-markup
 (fn [{:keys [db]} [_ document-id version]]
   {:db db
    :http-xhrio {:method          :get
                 :uri             (str "/archive/" (:content version))
                 :response-format (ajax/text-response-format)
                 :on-success      [::fetch-markup-success document-id]
                 :on-failure      [::fetch-failure]}}))

(rf/reg-event-db
 ::fetch-markup-success
 (fn [db [_ document-id markup]]
   (let [_ true]
     (-> db
         (assoc-in [:markup document-id] markup)
         (assoc-in [:loading :markup] false)))))

(rf/reg-event-db
 ::fetch-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-markup (get response :status-text))
       (assoc-in [:loading :markup] false))))

(rf/reg-sub
  ::markup
  (fn [db [_ document-id]] (get-in db [:markup document-id])))

(rf/reg-event-db
   ::set-markup
   (fn [db [_ document-id new-value]]
     (assoc-in db [:markup document-id] new-value)))

(defn markup [{id :id}]
  (let [loading? @(rf/subscribe [::notifications/loading? :markup])
        errors? @(rf/subscribe [::notifications/errors?])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        get-value (fn [e] (-> e .-target .-value))
        ;; the dispatch needs to be sync, otherwise the cursor will
        ;; jump to the end, see
        ;; https://dev.to/kwirke/solving-caret-jumping-in-react-inputs-36ic
        save!     #(rf/dispatch-sync [::set-markup id %])]
    (cond
      errors? [notifications/error-notification]
      loading? [notifications/loading-spinner]
      :else
      [:div.block
       [:div.field
        [:div.control
         [:textarea.textarea {:rows 20
                              :auto-focus true
                              :value @(rf/subscribe [::markup id])
                              :aria-label (tr [:markup])
                              :on-change #(save! (get-value %))}]]]
       [:div.field
        [:label.label (tr [:comment])]
        [:div.control
         [:input.input {:type "text" :placeholder (tr [:comment])}]]]
       
       [:div.field.is-grouped
        [:div.control
         (if @(rf/subscribe [::notifications/button-loading? id :markup])
           [:button.button.is-success.is-loading]
           [:button.button.is-success.has-tooltip-arrow
            {:disabled (not authenticated?)}
            [:span.icon {:aria-hidden true} [:i.mi.mi-save]]
            [:span (tr [:save])]])]]])))


