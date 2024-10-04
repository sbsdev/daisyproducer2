(ns daisyproducer2.documents
  (:require
    [ajax.core :as ajax]
    [re-frame.core :as rf]
    [daisyproducer2.documents.document :as document]
    [daisyproducer2.documents.state :as state]
    [daisyproducer2.words.notifications :as notifications]
    [daisyproducer2.i18n :refer [tr]]))

(rf/reg-event-fx
  ::fetch-documents
  (fn [{:keys [db]} [_ search]]
    {:db (notifications/set-loading db :documents)
     :http-xhrio {:method          :get
                  :uri             "/api/documents"
                  :response-format (ajax/json-response-format {:keywords? true})
                  :params          {:search (if (nil? search) "" search)}
                  :on-success      [::fetch-documents-success]
                  :on-failure      [::fetch-documents-failure]}}))

(rf/reg-event-db
 ::fetch-documents-success
 (fn [db [_ documents]]
   (-> db
       (notifications/clear-loading :documents)
       (assoc-in [:documents] documents))))

(rf/reg-event-db
 ::fetch-documents-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-documents (get response :status-text))
       (notifications/clear-loading :documents))))

(rf/reg-event-fx
  ::init-documents
  (fn [{:keys [db]} _]
    (let [search (:documents-search db)]
      {:dispatch [::fetch-documents search]})))

(rf/reg-event-fx
   ::search-change
   (fn [{:keys [db]} [_ new-search-value]]
     {:dispatch [::fetch-documents new-search-value]
      :db   (assoc db :documents-search new-search-value)}))

(rf/reg-sub
  ::documents
  (fn [db _] (-> db :documents)))

(rf/reg-sub
 ::documents-sorted
 :<- [::documents]
 (fn [documents] (->> documents (sort-by (juxt :title :author)))))

(rf/reg-sub
  ::search
  (fn [db _] (-> db :documents-search)))

(defn- search []
  (let [gettext (fn [e] (-> e .-target .-value))
        emit    (fn [e] (rf/dispatch [::search-change (gettext e)]))]
    [:div.field
     [:div.control
      [:input.input {:type "text"
                     :placeholder (tr [:search])
                     :aria-label (tr [:search])
                     :value @(rf/subscribe [::search])
                     :on-change emit}]]]))

(defn- document-link [{:keys [id title] :as document}]
  [:a {:href (str "#/documents/" id)
       :on-click (fn [_] (rf/dispatch [::document/set-current document]))}
   title])

(defn page []
  [:section.section>div.container>div.content
   [search]
   [:table.table.is-striped.is-fullwidth
    [:thead
     [:tr
      [:th (tr [:title])] [:th (tr [:author])] [:th (tr [:source-publisher])] [:th (tr [:state])]]]
    [:tbody
     (for [{:keys [id author source-publisher state] :as document} @(rf/subscribe [::documents-sorted])]
       ^{:key id}
       [:tr
        [:td [document-link document]]
        [:td author] [:td source-publisher]
        [:td [state/state state]]])]]])

