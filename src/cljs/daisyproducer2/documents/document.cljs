(ns daisyproducer2.documents.document
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::current
 (fn [db _]
   (-> db :current-document)))

(rf/reg-event-db
  ::set-current
  (fn [db [_ document]]
    (assoc db :current-document document)))

(rf/reg-event-fx
  ::fetch-current
  (fn [_ [_ id]]
    {:http-xhrio {:method          :get
                  :uri             (str "/api/documents/" id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-current]}}))

(rf/reg-event-fx
  ::init-current
  (fn [{:keys [db]} [_ id]]
    {:dispatch [::fetch-current id]}))


(defn tab-link [uri title page on-click]
  (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
    [:li.is-active [:a title]]
    [:li [:a {:href uri :on-click on-click} title]]))

(defn tabs [{:keys [id]}]
  [:div.block
   [:div.tabs.is-boxed
    [:ul
     [tab-link (str "#/documents/" id) (tr [:details]) :document]
     [tab-link (str "#/documents/" id "/unknown") (tr [:unknown-words]) :document-unknown (fn [_] (rf/dispatch [::unknown/fetch-words id]))]
     [tab-link (str "#/documents/" id "/local") (tr [:local-words]) :document-local (fn [_] (rf/dispatch [::local/fetch-words id]))]
     [tab-link (str "#/documents/" id "/preview") (tr [:preview]) :document-preview]
     ]]])

(defn summary [{:keys [title author source-publisher state-id]}]
  (let [state (state/mapping state-id state-id)]
    [:div.block
     [:table.table
      [:tbody
       [:tr [:th {:width 200} (tr [:title])] [:td title]]
       [:tr [:th (tr [:author])] [:td author]]
       [:tr [:th (tr [:source-publisher])] [:td source-publisher]]
       [:tr [:th (tr [:state])] [:td state]]]]]))

(defn details [document]
  [:div.block
   [:table.table.is-striped
    [:tbody
     (for [k [:date :modified-at :spelling :language]
           :let [v (case k
                     :spelling (state/mapping (get document k))
                     (get document k))]
           :when (not (string/blank? v))]
       ^{:key k}
       [:tr [:th (tr [k])] [:td v]])]]
   #_[:button.button.is-success
    (tr [:transitions-state] [(-> document :state-id state/next-mapping state/mapping)])]])

(defn- tooltip-button [{:keys [tooltip icon href label] :as opts}]
  ;; if we have an href we need an anchor element. Otherwise use a button
  (let [element (if href :a.button.has-tooltip-arrow :button.button.has-tooltip-arrow)]
    [element
     (merge
      {:data-tooltip (tr [tooltip])
       :aria-label (tr [tooltip])}
      (dissoc opts :tooltip :icon))
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]
     (when label [:span (tr [label])])]))

(defn- label-button [{:keys [label icon href] :as opts}]
  ;; if we have an href we need an anchor element. Otherwise use a button
  (let [element (if href :a.button :button.button)]
    [element
     (dissoc opts :label :icon)
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]
     (when label [:span (tr [label])])]))

(defn preview-links [document]
  [:div.blockxo
   [:table.table
    [:tbody
     ;; FIXME: not implemented yet
     #_[:tr
      [:th (tr [:braille])]
      [:td [:div.field.is-grouped
            [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]]]]
     [:tr
      [:th (tr [:epub3])]
      [:td [:div.field.is-grouped
            [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]
            [:p.control [label-button {:label :open-in-online-player :icon "mi-open-in-new" }]]]]]]]])

(defn page []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [details document]]))

(defn unknown []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [grade/selector ::unknown/fetch-words]
     [unknown/unknown-words [::current]]]))

(defn local []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [grade/selector ::local/fetch-words]
     [local/local-words [::current]]]))

(defn preview []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [preview-links document]]))

