(ns daisyproducer2.documents.preview
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::generate-epub
  (fn [{:keys [db]} [_ id product-id]]
    {:db (notifications/set-button-state db id :generate-epub)
     :http-xhrio {:method          :get
                  :uri             (str "api/documents/" id "/preview/epub-in-player")
                  :params          {:product-id product-id}
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::ack-generate-epub id]
                  :on-failure      [::ack-failure id :generate-epub]
                  }}))

(rf/reg-event-fx
  ::ack-generate-epub
  (fn [{:keys [db]} [_ id {location :location}]]
    {:db (notifications/clear-button-state db id :generate-epub)
     :dispatch [::redirect-online-player location]}))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id request-type response]]
   (-> db
       (assoc-in [:errors request-type] (or (get-in response [:response :status-text])
                                            (get response :status-text)))
       (notifications/clear-button-state id request-type))))

(rf/reg-event-fx
  ::redirect-online-player
  (fn [{:keys [db]} [_ location]]
    {:db db
     :open-window location}))

(rf/reg-fx
  :open-window
  (fn [uri]
    (.open js/window uri "_blank")))

(defn preview-links [{id :id}]
  [:div.block
     [:table.table
      [:thead
       [:tr
        [:th {:width "100%"} (tr [:format])]
        [:th (tr [:action])]]]
      [:tbody
       [:tr
        [:th (tr [:epub3])]
        [:td [:div.field.is-grouped
              [:p.control
               [:a.button
                {:href (str "/api/documents/" id "/preview/epub")
                 ;;:download "download"
                 :target "_blank"}
                [:span (tr [:download])]
                [:span.icon {:aria-hidden true}
                 [:i.mi.mi-download]]]]
              [:p.control
               (let [loading? @(rf/subscribe [::notifications/button-loading? id :generate-epub])]
                 [:button.button
                  {:class (when loading? "is-loading")
                   :on-click #(rf/dispatch [::generate-epub id "EB12951"])}
                  [:span (tr [:online-player])]
                  [:span.icon {:aria-hidden true}
                   [:i.mi.mi-open-in-new]]])]]]]]]])


