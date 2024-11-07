(ns daisyproducer2.documents.image
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.words.notifications :as notifications]
            [daisyproducer2.progress :as progress]
            [re-frame.core :as rf]))

(defn- get-search [db document-id] (get-in db [:search :images document-id]))

(rf/reg-event-fx
  ::fetch-images
  (fn [{:keys [db]} [_ document-id]]
    (let [search (get-search db document-id)]
      {:db (assoc-in db [:loading :images] true)
       :http-xhrio (as-transit
                    {:method          :get
                     :uri             (str "/api/documents/" document-id "/images")
                     :params          (if (str/blank? search) {} {:search search})
                     :on-success      [::fetch-images-success]
                     :on-failure      [::fetch-images-failure]})})))

(rf/reg-event-db
 ::fetch-images-success
 (fn [db [_ images]]
   (let [images (map #(assoc % :uuid (str (random-uuid))) images)]
     (-> db
         (assoc-in [:images] (zipmap (map :uuid images) images))
         (assoc-in [:loading :images] false)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-images-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-images (get response :status-text))
       (assoc-in [:loading :images] false))))

(rf/reg-sub
  ::images
  (fn [db _] (->> db :images vals)))

(rf/reg-sub
 ::images-sorted
 :<- [::images]
 (fn [images] (->> images (sort-by :content))))

(rf/reg-sub
 ::images?
 :<- [::images]
 (fn [images] (seq images)))

(rf/reg-event-fx
  ::delete-all-images
  (fn [{:keys [db]} [_ document-id]]
    {:db (-> db
             (assoc-in [:loading :images] true)
             (notifications/set-button-state :all-images :delete))
     :http-xhrio (as-transit
                  {:method          :delete
                   :headers 	    (auth/auth-header db)
                   :uri             (str "/api/documents/" document-id "/images")
                   :on-success      [::delete-images-success document-id]
                   :on-failure      [::delete-images-failure]})}))

(rf/reg-event-fx
 ::delete-images-success
 (fn [{:keys [db]} [_ document-id]]
   {:db (-> db
            (assoc-in [:loading :images] false)
            (notifications/clear-button-state :all-images :delete))
    :dispatch [::fetch-images document-id]}))

(rf/reg-event-db
 ::delete-images-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :delete-all-images (get response :status-text))
       (assoc-in [:loading :images] false)
       (notifications/clear-button-state :all-images :delete))))

(rf/reg-event-fx
 ::delete-image
 (fn [{:keys [db]} [_ uuid]]
   (let [{:keys [id document-id]} (get-in db [:images uuid])]
     {:db (notifications/set-button-state db uuid :delete)
      :http-xhrio (as-transit
                   {:method          :delete
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" document-id "/images/" id)
                    :on-success      [::ack-delete uuid]
                    :on-failure      [::ack-failure-delete uuid :delete]})})))

(rf/reg-event-db
 ::ack-delete
 (fn [db [_ uuid]]
   (-> db
       (update-in [:images] dissoc uuid)
       (notifications/clear-button-state uuid :delete))))

(rf/reg-event-db
 ::ack-failure-delete
 (fn [db [_ uuid request-type response]]
   (-> db
       (notifications/set-errors :delete-image (get response :status-text))
       (notifications/clear-button-state uuid request-type))))

(defn delete-all-images-button [document-id]
  (let [authenticated? @(rf/subscribe [::auth/authenticated?])
        has-images? @(rf/subscribe [::images?])]
    [:div.buttons.has-addons.is-right
     [:button.button.is-danger
      {:disabled (not (and authenticated? has-images?))
       :aria-label (tr [:delete-all-images])
       :on-click (fn [e] (rf/dispatch [::delete-all-images document-id]))}
      [:span.icon {:aria-hidden true} [:i.mi.mi-delete]]
      [:span (tr [:delete-all-images])]]]))

(rf/reg-sub ::search (fn [db [_ document-id]] (get-search db document-id) ))

(rf/reg-event-fx
   ::set-search
   (fn [{:keys [db]} [_ document-id new-search-value]]
     {:db (assoc-in db [:search :images document-id] new-search-value)
      :dispatch [::fetch-images document-id]}))

(defn image-search [document-id]
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

(defn image-upload [document-id]
  (let [authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:a.button.is-primary
     {:href (when authenticated? (str "#/documents/" document-id "/images/upload"))
      :aria-label (tr [:new-image])
      :disabled (not authenticated?)}
     [:span.icon {:aria-hidden true} [:i.mi.mi-upload]]
     [:span (tr [:new-image])]]))

(defn image-filter [document-id]
  [:div.field.is-horizontal
   [:div.field-body
    [:div.field.is-grouped
     [:p.control.is-expanded
      [image-search document-id]]
     [:p.control
      [image-upload document-id]]
     [:div.control
      [delete-all-images-button document-id]]]]])

;; see https://www.dotkam.com/2012/11/23/convert-html5-filelist-to-clojure-vector/
(defn toArray [js-col]
  (-> (clj->js [])
      (.-slice)
      (.call js-col)
      (js->clj)))

(defn- extract-filenames [images]
  (map (fn [img] (.-name img)) images))

(rf/reg-event-fx
  ::add-image
  (fn [{:keys [db]} [_ document js-file-value]]
    (let [name (.-name js-file-value)
          form-data (doto (js/FormData.)
                      (.append "file" js-file-value name))]
      {:http-xhrio (as-transit
                    {:method          :post
                     :headers 	     (auth/auth-header db)
                     :uri             (str "/api/documents/" (:id document) "/images")
                     :body            form-data
                     :on-success      [::ack-add-image document name]
                     :on-failure      [::ack-failure-image document name]})})))

(rf/reg-event-fx
 ::add-all-images
 (fn [{:keys [db]} [_ document images]]
   {:db (progress/init-progress db :upload (count images))
    :fx (mapv (fn [img] [:dispatch [::add-image document img]]) images)}))

(rf/reg-event-fx
 ::ack-add-image
 (fn [{:keys [db]} [_ document name]]
   (let [db (progress/update-progress db :upload)
         {:keys [value max]} (-> db (get-in [:progress :upload]))
         done? (>= value max)]
     (if done?
       {:db db
        :fx [[:dispatch [::fetch-images (:id document)]]
             [:dispatch [:common/navigate! :document-images document]]]}
       {:db db}))))

(rf/reg-event-db
 ::ack-failure-image
 (fn [db [_ document name response]]
   (-> db
       (notifications/set-errors :save (or (get-in response [:response :status-text])
                                           (get response :status-text)))
       ;; FIXME: are we not doing a dispatch to the images list view when an
       ;; error happens?
       (progress/update-progress :upload))))

(rf/reg-sub
 ::image-files
 (fn [db _] (-> db :image-upload :files)))

(rf/reg-sub
 ::image-file-names
 :<- [::image-files]
 (fn [files] (->> files (map #(.-name %)) (str/join ", "))))

(rf/reg-event-db
  ::set-image-files
  (fn [db [_ files]] (assoc-in db [:image-upload :files] files)))

(defn- image-files []
  (let [get-value (fn [e] (-> e .-target .-files))
        save!     #(rf/dispatch-sync [::set-image-files (toArray %)])
        files     @(rf/subscribe [::image-files])
        names     @(rf/subscribe [::image-file-names])]
    [:p.control
     [:div.file.has-name.is-fullwidth
      [:label.file-label
       [:input.file-input
        {:type "file"
         :accept ".jpeg,.jpg,.png"
         :multiple "multiple"
         :files files
         :on-change #(save! (get-value %))}]
       [:span.file-cta
        [:span.file-icon [:i.mi.mi-file-upload]]
        [:span.file-label (tr [:choose-images])]]
       [:span.file-name (if files names (tr [:no-file]))]]]]))

(defn upload [document]
  (let [files @(rf/subscribe [::image-files])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        klass (when @(rf/subscribe [::progress/in-progress? :upload]) "is-loading")
        errors? @(rf/subscribe [::notifications/errors?])]
    (if errors?
      [notifications/error-notification]
      [:<>
       [progress/progress-bar :upload]
       [:div.field.is-grouped-multiline
        [image-files]
        [:p.control
         [:button.button.is-success
          {:disabled (or (nil? files) (not authenticated?))
           :class klass
           :on-click (fn [e] (rf/dispatch [::add-all-images document files]))}
          [:span.icon {:aria-hidden true} [:i.mi.mi-backup]]
          [:span (tr [:upload])]]]]])))

(defn buttons [id]
  (let [authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:div.buttons.has-addons
     (if @(rf/subscribe [::notifications/button-loading? id :delete])
       [:button.button.is-danger.is-loading]
       [:button.button.is-danger.has-tooltip-arrow
        {:disabled (not authenticated?)
         :data-tooltip (tr [:delete])
         :aria-label (tr [:delete])
         :on-click (fn [e] (rf/dispatch [::delete-image id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-delete]]])]))

(defn- image-row [{:keys [uuid content]}]
  [:tr
   [:td
    (let [href (str "/archive/" content)
          file-name (last (str/split content #"/"))]
      [:a {:href href :target "_blank"} file-name])]
   [:td {:width "5%"} [buttons uuid]]])

(defn images [document]
  (let [loading? @(rf/subscribe [::notifications/loading? :images])
        errors? @(rf/subscribe [::notifications/errors?])]
    [:<>
     [image-filter (:id document)]
     (cond
       errors? [notifications/error-notification]
       :else
       [:<>
        [:table.table.is-striped.is-fullwidth
         [:thead
          [:tr
           [:th (tr [:image])]
           [:th (tr [:action])]]]
         [:tbody
          (for [{:keys [uuid] :as image} @(rf/subscribe [::images-sorted])]
            ^{:key uuid} [image-row image])]]])]))
