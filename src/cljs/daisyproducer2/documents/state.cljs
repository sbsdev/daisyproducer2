(ns daisyproducer2.documents.state
  (:require
   [daisyproducer2.ajax :refer [as-transit]]
   [daisyproducer2.auth :as auth]
   [daisyproducer2.i18n :refer [tr]]
   [daisyproducer2.words.notifications :as notifications]
   [re-frame.core :as rf]))

;; define a bulma tag class for each state, see https://bulma.io/documentation/elements/tag/
(def klass {"open" "is-success" "closed" "is-info"})

(defn state
  "Component to diplay the state in a Bulma tag"
  [state]
  (let [klass (klass state)]
    [:span.tag {:class klass} state]))

(rf/reg-sub
 ::current-state
 :<- [:daisyproducer2.documents.document/current]
 (fn [current] (->> current :state)))

(rf/reg-event-db
  ::set-current-state
  (fn [db [_ state]] (assoc-in db [:current-document :state] state)))

(rf/reg-event-fx
  ::update-state
  (fn [{:keys [db]} [_ document state]]
    {:db (notifications/set-button-state db :document :update)
     :http-xhrio (as-transit
                  {:method          :patch
                   :headers 	     (auth/auth-header db)
                   :uri             (str "/api/documents/" (:id document))
                   :params          {:state state}
                   :on-success      [::ack-update-state state]
                   :on-failure      [::ack-failure]})}))

(rf/reg-event-fx
  ::ack-update-state
  (fn [{:keys [db]} [_ state]]
    {:db (-> db (notifications/clear-button-state :document :update))
     :dispatch [::set-current-state state]}))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ response]]
   (let [message (or (get-in response [:response :status-text])
                     (get response :status-text))
         errors (get-in response [:response :errors])]
     (-> db
         (notifications/set-errors :document message errors)
         (notifications/clear-button-state :document :update)))))


(defn button
  "Button to change the state of a production"
  [document]
  (let [admin? @(rf/subscribe [::auth/is-admin?])
        state @(rf/subscribe [::current-state])]
    (when admin?
      (if (= state "open")
        [:button.button.is-success
         {:on-click (fn [e] (rf/dispatch [::update-state document "closed"]))}
         [:span.icon {:aria-hidden true}
          [:i.mi.mi-close]]
         [:span (tr [:close])]]
        [:button.button.is-success.is-light
         {:on-click (fn [e] (rf/dispatch [::update-state document "open"]))}
         [:span.icon {:aria-hidden true}
          [:i.mi.mi-refresh]]
         [:span (tr [:reopen])]]))))

;; FIXME: if we want state information for a document we need to have
;; the documents addressable with a uuid, i.e. we need to add a uuid
;; when loading the documents
#_(defn button-brief
  "Icon only button to change the state of a production"
  [uuid]
  (let [admin? @(rf/subscribe [::auth/is-admin?])
        documents @(rf/subscribe [:daisyproducer2.documents/documents])
        state-id (get-in documents [uuid :state-id])
        _ (println id state-id)]
    (when admin?
      (if (open? state-id)
        [:button.button.is-success.has-tooltip-arrow.is-small
         {:data-tooltip (tr [:close])
         :aria-label (tr [:close])}
         [:span.icon {:aria-hidden true}
          [:i.mi.mi-close]]]
        [:button.button.is-success.has-tooltip-arrow.is-small.is-light
         {:data-tooltip (tr [:reopen])
         :aria-label (tr [:reopen])}
         [:span.icon {:aria-hidden true}
          [:i.mi.mi-refresh]]]))))
