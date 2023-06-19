(ns daisyproducer2.documents.version
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [cljs-time.format :as tf]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-versions
  (fn [{:keys [db]} [_ document-id]]
    (let [offset (pagination/offset db :versions)]
      {:db (assoc-in db [:loading :versions] true)
       :http-xhrio (as-transit
                    {:method          :get
                     :uri             (str "/api/documents/" document-id "/versions")
                     :params          {:offset offset
                                       :limit pagination/page-size}
                     :on-success      [::fetch-versions-success]
                     :on-failure      [::fetch-versions-failure]})})))

(rf/reg-event-db
 ::fetch-versions-success
 (fn [db [_ versions]]
   (let [versions (->> versions
                       (map #(assoc % :uuid (str (random-uuid)))))
         next? (-> versions count (= pagination/page-size))]
     (-> db
         (assoc-in [:versions] (zipmap (map :uuid versions) versions))
         (pagination/update-next :versions next?)
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
 (fn [versions] (->> versions (sort-by :created-at))))

(rf/reg-event-fx
  ::add-version
  (fn [{:keys [db]} [_ id js-file-value comment]]
    (let [form-data (doto (js/FormData.)
                      (.append "comment" comment)
                      (.append "file" js-file-value "filename.txt"))]
      {:db (notifications/set-button-state db :version :save)
       :http-xhrio {:method          :post
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" id "/versions")
                    :body            form-data
                    :response-format (ajax/raw-response-format)
                    :on-success      [::ack-add-version]
                    :on-failure      [::ack-failure]
                    }})))

(rf/reg-event-db
  ::ack-add-version
  (fn [db [_]]
    (-> db (notifications/clear-button-state :version :save))))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ response]]
   (-> db
       (assoc-in [:errors :version] (or (get-in response [:response :status-text])
                                        (get response :status-text)))
       (notifications/clear-button-state :version :save))))

(rf/reg-sub
 ::version-file
 (fn [db _] (-> db :version-file)))

(rf/reg-event-db
  ::set-version-file
  (fn [db [_ file]] (assoc db :version-file file)))

(rf/reg-sub
 ::version-comment
 (fn [db _] (-> db :version-comment)))

(rf/reg-event-db
  ::set-version-comment
  (fn [db [_ comment]] (assoc db :version-comment comment)))

(defn- version-comment []
  (let [get-value (fn [e] (-> e .-target .-value))
        reset!    #(rf/dispatch [::set-version-comment ""])
        save!     #(rf/dispatch [::set-version-comment %])
        comment @(rf/subscribe [::version-comment])]
    [:p.control
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
    [:p.control
     [:div.file.has-name.is-fullwidth
      [:label.file-label
       [:input.file-input
        {:type "file"
         :accept ".xml"
         :files file
         :on-change #(save! (get-value %))}]
       [:span.file-cta
        [:span.file-icon [:i.mi.mi-file-upload]]
        [:span.file-label (tr [:choose-dtbook])]]
       [:span.file-name (if file (.-name file) (tr [:no-file]))]]]]))

(defn upload [id]
  (let [file @(rf/subscribe [::version-file])
        comment @(rf/subscribe [::version-comment])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        klass (when @(rf/subscribe [::notifications/button-loading? :version :save]) "is-loading")
        errors? @(rf/subscribe [::notifications/errors?])]
    (if errors?
      [notifications/error-notification]
      [:div.field.is-grouped
       [version-file]
       [version-comment]
       [:p.control
        [:button.button.is-success
         {:disabled (or (string/blank? comment) (nil? file) (not authenticated?))
          :class klass
          :on-click (fn [e] (rf/dispatch [::add-version id file comment]))}
         [:span.icon {:aria-hidden true} [:i.mi.mi-backup]]
         [:span (tr [:upload])]]]])))

(defn version-row [{:keys [content created-at created-by comment]}]
  [:tr
   [:td content]
   [:td comment]
   [:td created-by]
   [:td (when created-at (tf/unparse (tf/formatters :date) created-at))]
   [:td]])

(defn versions [id]
  (let [errors? @(rf/subscribe [::notifications/errors?])
        versions @(rf/subscribe [::versions-sorted])]
    (if errors?
      [notifications/error-notification]
      [:table.table.is-striped
       [:thead
        [:tr
         [:th (tr [:version])]
         [:th (tr [:comment])]
         [:th (tr [:author])]
         [:th (tr [:created-at])]
         [:th (tr [:action])]]]
       [:tbody
        (for [{:keys [uuid] :as version} versions]
          ^{:key uuid} [version-row version])]])))
