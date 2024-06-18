(ns daisyproducer2.documents.product
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.words.notifications :as notifications]
            [daisyproducer2.progress :as progress]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-products
  (fn [{:keys [db]} [_ document-id]]
    {:db (assoc-in db [:loading :products] true)
     :http-xhrio (as-transit
                  {:method          :get
                   :uri             (str "/api/documents/" document-id "/products")
                   :on-success      [::fetch-products-success]
                   :on-failure      [::fetch-products-failure]})}))

(rf/reg-event-db
 ::fetch-products-success
 (fn [db [_ products]]
   (let [products (map #(assoc % :uuid (str (random-uuid))) products)]
     (-> db
         (assoc-in [:products] (zipmap (map :uuid products) products))
         (assoc-in [:loading :products] false)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-products-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-products (get response :status-text))
       (assoc-in [:loading :products] false))))

(rf/reg-sub
  ::products
  (fn [db _] (->> db :products vals)))

(rf/reg-sub
 ::products-sorted
 :<- [::products]
 (fn [products] (->> products (sort-by :identifier))))

(rf/reg-sub
 ::products?
 :<- [::products]
 (fn [products] (seq products)))

(defn- product-row [{:keys [identifier]}]
  [:tr
   [:td identifier]])

(defn products [document]
  (let [loading? @(rf/subscribe [::notifications/loading? :products])
        errors? @(rf/subscribe [::notifications/errors?])
        products? @(rf/subscribe [::products?])]
    (cond
      errors? [notifications/error-notification]
      products? [:div.block
                 [:table.table.is-striped
                  [:thead
                   [:tr
                    [:th (tr [:products])]]]
                  [:tbody
                   (for [{:keys [uuid] :as product} @(rf/subscribe [::products-sorted])]
                     ^{:key uuid} [product-row product])]]])))
