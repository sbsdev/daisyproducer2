(ns daisyproducer2.documents.version
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(defn- get-search [db document-id] (get-in db [:search :versions document-id]))

(rf/reg-event-fx
  ::fetch-versions
  (fn [{:keys [db]} [_ document-id]]
    (let [search (get-search db document-id)]
      {:db (assoc-in db [:loading :versions] true)
       :http-xhrio (as-transit
                    {:method          :get
                     :uri             (str "/api/documents/" document-id "/versions")
                     :params          (if (string/blank? search) {} {:search search})
                     :on-success      [::fetch-versions-success]
                     :on-failure      [::fetch-versions-failure]})})))

(rf/reg-event-db
 ::fetch-versions-success
 (fn [db [_ versions]]
   (let [versions (map #(assoc % :uuid (str (random-uuid))) versions)]
     (-> db
         (assoc-in [:versions] (zipmap (map :uuid versions) versions))
         (assoc-in [:loading :versions] false)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-versions-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-versions (get response :status-text))
       (assoc-in [:loading :versions] false))))

(rf/reg-sub
  ::versions
  (fn [db _] (->> db :versions vals)))

(rf/reg-sub
 ::versions-sorted
 :<- [::versions]
 (fn [versions] (->> versions (sort-by (comp tc/to-epoch :created-at) >))))

(rf/reg-sub
 ::multiple-versions?
 :<- [::versions]
 (fn [versions] (-> versions count (> 1))))

(rf/reg-event-fx
  ::add-version
  (fn [{:keys [db]} [_ document js-file-value comment]]
    (let [form-data (doto (js/FormData.)
                      (.append "comment" comment)
                      (.append "file" js-file-value "filename.txt"))]
      {:db (notifications/set-button-state db :version :save)
       :http-xhrio (as-transit
                    {:method          :post
                     :headers 	     (auth/auth-header db)
                     :uri             (str "/api/documents/" (:id document) "/versions")
                     :body            form-data
                     :on-success      [::ack-add-version document]
                     :on-failure      [::ack-failure]})})))

(rf/reg-event-fx
  ::ack-add-version
  (fn [{:keys [db]} [_ document]]
    {:db (-> db (notifications/clear-button-state :version :save))
     :dispatch-n (list
                  [::fetch-versions (:id document)]
                  [:common/navigate! :document-versions document])}))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ response]]
   (let [message (or (get-in response [:response :status-text])
                     (get response :status-text))
         errors (get-in response [:response :errors])]
     (-> db
         (notifications/set-errors :version message errors)
         (notifications/clear-button-state :version :save)))))

(rf/reg-sub
 ::version-file
 (fn [db _] (-> db :version-upload :file)))

(rf/reg-event-db
  ::set-version-file
  (fn [db [_ file]] (assoc-in db [:version-upload :file] file)))

(rf/reg-sub
 ::version-comment
 (fn [db _] (-> db :version-upload :comment)))

(rf/reg-event-db
  ::set-version-comment
  (fn [db [_ comment]] (assoc-in db [:version-upload :comment] comment)))

(defn- version-comment []
  (let [get-value (fn [e] (-> e .-target .-value))
        reset!    #(rf/dispatch [::set-version-comment ""])
        save!     #(rf/dispatch [::set-version-comment %])
        comment @(rf/subscribe [::version-comment])]
    [:div.field
     [:label.label (tr [:comment])]
     [:input.input
      {:type "text"
       :placeholder (tr [:comment])
       :aria-label (tr [:comment])
       :value comment
       :on-change #(save! (get-value %))
       :on-key-down #(when (= (.-which %) 27) (reset!))
       }]]))

(defn- version-file []
  (let [get-value (fn [e] (-> e .-target .-files (aget 0)))
        save!     #(rf/dispatch [::set-version-file %])
        file      @(rf/subscribe [::version-file])]
    [:div.field
     [:label.label (tr [:file])]
     [:div.file.has-name
      [:label.file-label
       [:input.file-input
        {:type "file"
         :accept ".xml"
         :files file
         :on-change #(save! (get-value %))}]
       [:span.file-cta
        [:span.file-icon [:i.mi.mi-file-upload]]
        [:span.file-label (tr [:choose-file])]]
       [:span.file-name (if file (.-name file) (tr [:no-file]))]]]]))

(defn upload [document]
  (let [file @(rf/subscribe [::version-file])
        comment @(rf/subscribe [::version-comment])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        klass (when @(rf/subscribe [::notifications/button-loading? :version :save]) "is-loading")
        errors? @(rf/subscribe [::notifications/errors?])]
    (if errors?
      [notifications/error-notification]
      [:div.field
       [version-file]
       [version-comment]
       [:div.control
        [:button.button.is-success
         {:disabled (or (string/blank? comment) (nil? file) (not authenticated?))
          :class klass
          :on-click (fn [e] (rf/dispatch [::add-version document file comment]))}
         [:span.icon {:aria-hidden true} [:i.mi.mi-backup]]
         [:span (tr [:upload])]]]])))

(rf/reg-sub ::search (fn [db [_ document-id]] (get-search db document-id) ))

(rf/reg-event-fx
   ::set-search
   (fn [{:keys [db]} [_ document-id new-search-value]]
     {:db (assoc-in db [:search :versions document-id] new-search-value)
      :dispatch [::fetch-versions document-id]}))

(defn version-search [document-id]
  (let [get-value (fn [e] (-> e .-target .-value))
        reset!    #(rf/dispatch [::set-search document-id ""])
        save!     #(rf/dispatch [::set-search document-id %])]
    [:div.field
     [:div.control
      [:input.input {:type "text"
                     :placeholder (tr [:search])
                     :aria-label (tr [:search])
                     :value @(rf/subscribe [::search document-id])
                     :on-change #(save! (get-value %))
                     :on-key-down #(when (= (.-which %) 27) (reset!))}]]]))

(defn version-upload [document-id]
  (let [authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:a.button.is-primary.has-tooltip-arrow
     {:href (str "#/documents/" document-id "/versions/upload")
      :data-tooltip (tr [:new-version])
      :aria-label (tr [:new-version])
      :disabled (not authenticated?)}
     [:span.icon {:aria-hidden true}
      [:i.mi {:class "mi-upload"}]]]))

(defn version-filter [document-id]
  [:div.field.is-horizontal
   [:div.field-body
    [:div.field.is-grouped
     [:div.control.is-expanded
      [version-search document-id]]
     [:div.control
      [version-upload document-id]]]]])

(defn- version-row [{:keys [content created-at created-by comment]}]
  [:tr
   [:td
    (let [href (str "/archive/" content)]
      [:a {:href href :target "_blank"} comment])]
   [:td created-by]
   [:td {:width "12%"} (when created-at (tf/unparse (tf/formatter "yyyy-MM-dd HH:mm") created-at))]])

(defn versions [document]
  (let [errors? @(rf/subscribe [::notifications/errors?])
        versions @(rf/subscribe [::versions-sorted])]
    [:<>
     [version-filter (:id document)]
     (if errors?
       [notifications/error-notification]
       [:<>
        [:table.table.is-striped
         [:thead
          [:tr
           [:th (tr [:comment])]
           [:th (tr [:author])]
           [:th (tr [:created-at])]]]
         [:tbody
          (for [{:keys [uuid] :as version} versions]
            ^{:key uuid} [version-row version])]]])]))
