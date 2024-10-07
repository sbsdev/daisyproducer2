(ns daisyproducer2.documents.markup
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.documents.version :as version]
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
         (assoc :markup {:content markup :comment ""})
         (assoc-in [:loading :markup] false)))))

(rf/reg-event-db
 ::fetch-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-markup (get response :status-text))
       (dissoc :markup) ; drop contents that might remain from other documents
       (assoc-in [:loading :markup] false))))

(rf/reg-event-fx
  ::add-markup
  (fn [{:keys [db]} [_ document]]
    (let [markup (get-in db [:markup :content])
          comment (get-in db [:markup :comment])
          blob (js/Blob. [markup] {:type "plain/text"})
          form-data (doto (js/FormData.)
                      (.append "comment" comment)
                      (.append "file" blob "not_a_real_file.xml"))]
      {:db (notifications/set-button-state db :markup :save)
       :http-xhrio (as-transit
                    {:method          :post
                     :headers 	     (auth/auth-header db)
                     :uri             (str "/api/documents/" (:id document) "/versions")
                     :body            form-data
                     :on-success      [::ack-add-markup document]
                     :on-failure      [::ack-failure]})})))

(rf/reg-event-fx
  ::ack-add-markup
  (fn [{:keys [db]} [_ document]]
    {:db (-> db (notifications/clear-button-state :markup :save))
     :dispatch-n (list
                  [::version/fetch-versions (:id document)]
                  [:common/navigate! :document-versions document])}))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ response]]
   (let [message (or (get-in response [:response :status-text])
                     (get response :status-text))
         errors (get-in response [:response :errors])
         status (get response :status)]
     (-> db
         (notifications/clear-button-state :markup :save)
         (cond-> (= status 400)
           ;; presumably a validation problem
           (assoc-in [:markup :validation] {:message message :errors errors}))
         (cond-> (not= status 400)
           ;; some other problem
           (notifications/set-errors :markup message errors))))))


(rf/reg-sub
  ::markup
  (fn [db [_]] (get-in db [:markup :content])))

(rf/reg-event-db
   ::set-markup
   (fn [db [_ new-value]]
     (assoc-in db [:markup :content] new-value)))

(rf/reg-sub
 ::markup-comment
 (fn [db _] (-> db :markup :comment)))

(rf/reg-sub
 ::markup-comment?
 :<- [::markup-comment]
 (fn [comment] (not (str/blank? comment))))

(rf/reg-event-db
  ::set-markup-comment
  (fn [db [_ comment]] (assoc-in db [:markup :comment] comment)))

(rf/reg-event-db
 ::ack-validation
 (fn [db [_]]
   (update db :markup dissoc :validation)))

(rf/reg-sub
  ::markup-validation
  (fn [db [_]] (get-in db [:markup :validation])))

(rf/reg-sub
 ::markup-valid?
 :<- [::markup-validation]
 (fn [validation] (empty? validation)))

(defn- markup-comment []
  (let [get-value (fn [e] (-> e .-target .-value))
        reset!    #(rf/dispatch [::set-markup-comment ""])
        save!     #(rf/dispatch [::set-markup-comment %])
        comment @(rf/subscribe [::markup-comment])]
    [:div.field
     [:label.label (tr [:comment])]
     [:div.control
      [:input.input
       {:type "text"
        :placeholder (tr [:comment])
        :aria-label (tr [:comment])
        :value comment
        :on-change #(save! (get-value %))
        :on-key-down #(when (= (.-which %) 27) (reset!))}]]]))

(defn- markup-textarea []
  (let [get-value (fn [e] (-> e .-target .-value))
        klass (when-not @(rf/subscribe [::markup-valid?]) "is-danger")
        ;; the dispatch needs to be sync, otherwise the cursor will
        ;; jump to the end, see
        ;; https://dev.to/kwirke/solving-caret-jumping-in-react-inputs-36ic
        save!     #(rf/dispatch-sync [::set-markup %])
        content   @(rf/subscribe [::markup])]
    [:div.field
     [:div.control
      [:textarea.textarea
       {:class klass
        :rows 30
        :value content
        :aria-label (tr [:markup])
        :on-change #(save! (get-value %))}]]]))

(defn markup-notification []
  (when-let [problems @(rf/subscribe [::markup-validation])]
    (let [{:keys [message errors]} problems]
      [:div.block
       [:div.notification.is-danger
        [:button.delete
         {:on-click (fn [e] (rf/dispatch [::ack-validation]))}]
        [:p [:strong message]]
        (if (seq errors)
          [:ul
           (for [e errors]
             ^{:key e}
             [:li (str e)])]
          [:p (str errors)])]])))

(defn markup [{id :id :as document}]
  (let [loading? @(rf/subscribe [::notifications/loading? :markup])
        errors? @(rf/subscribe [::notifications/errors?])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        valid? @(rf/subscribe [::markup-valid?])
        comment? @(rf/subscribe [::markup-comment?])
        klass (when @(rf/subscribe [::notifications/button-loading? :markup :save]) "is-loading")]
    (cond
      errors? [notifications/error-notification]
      loading? [notifications/loading-spinner]
      :else
      [:div.block
       [markup-textarea]
       [markup-notification]
       [markup-comment]
       [:div.field.is-grouped
        [:div.control
         [:button.button.is-success.has-tooltip-arrow
          {:disabled (or errors? (not valid?) (not comment?) (not authenticated?))
           :class klass
           :on-click (fn [e] (rf/dispatch [::add-markup document]))}
          [:span.icon {:aria-hidden true} [:i.mi.mi-save]]
          [:span (tr [:save])]]]]])))


