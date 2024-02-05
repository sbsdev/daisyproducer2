(ns daisyproducer2.documents.image
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(defn- get-search [db document-id] (get-in db [:search :images document-id]))

(rf/reg-event-fx
  ::fetch-images
  (fn [{:keys [db]} [_ document-id]]
    (let [offset (pagination/offset db :images)
          search (get-search db document-id)]
      {:db (assoc-in db [:loading :images] true)
       :http-xhrio (as-transit
                    {:method          :get
                     :uri             (str "/api/documents/" document-id "/images")
                     :params          (cond-> {:offset offset
                                               :limit pagination/page-size}
                                        (not (string/blank? search)) (assoc :search search))
                     :on-success      [::fetch-images-success]
                     :on-failure      [::fetch-images-failure]})})))

(rf/reg-event-db
 ::fetch-images-success
 (fn [db [_ images]]
   (let [images (->> images
                     (map #(assoc % :uuid (str (random-uuid)))))
         next? (-> images count (= pagination/page-size))]
     (-> db
         (assoc-in [:images] (zipmap (map :uuid images) images))
         (pagination/update-next :images next?)
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

(rf/reg-event-fx
 ::delete-image
 (fn [{:keys [db]} [_ uuid]]
   (let [{:keys [id document-id]} (get-in db [:images uuid])]
     {:db (notifications/set-button-state db uuid :delete)
      :http-xhrio {:method          :delete
                   :format          (ajax/json-request-format)
                   :headers 	     (auth/auth-header db)
                   :uri             (str "/api/documents/" document-id "/images/" id)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [::ack-delete uuid document-id]
                   :on-failure      [::ack-failure uuid :delete]
                   }})))

(rf/reg-event-fx
 ::ack-delete
 (fn [{:keys [db]} [_ uuid document-id]]
   (let [db (-> db
                (update-in [:images document-id] dissoc uuid)
                (notifications/clear-button-state uuid :delete))
         empty? (-> db (get-in [:images document-id]) count (< 1))]
     (if empty?
       {:db db :dispatch [::fetch-images document-id]}
       {:db db}))))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ uuid request-type response]]
   (-> db
       (assoc-in [:errors request-type] (or (get-in response [:response :status-text])
                                            (get response :status-text)))
       (notifications/clear-button-state uuid request-type))))

(rf/reg-sub ::search (fn [db [_ document-id]] (get-search db document-id) ))

(rf/reg-event-fx
   ::set-search
   (fn [{:keys [db]} [_ document-id new-search-value]]
     {:db (assoc-in db [:search :images document-id] new-search-value)
      :dispatch-n (list
                   ;; when searching for a new image reset the pagination
                   [::pagination/reset :images]
                   [::fetch-images document-id])}))

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
     {:href (str "#/documents/" document-id "/images/upload")
      :disabled (not authenticated?)}
     [:span.icon {:aria-hidden true}
      [:i.mi {:class "mi-upload"}]]
     [:span (tr [:new-image])]]))

(defn image-filter [document-id]
  [:div.field.is-horizontal
   [:div.field-body
    [:div.field.is-grouped
     [:p.control.is-expanded
      [image-search document-id]]
     [:p.control
      [image-upload document-id]]]]])

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
   {:db (notifications/set-upload-state db (:id document) (set (extract-filenames images)))
    ;; FIXME: :dispatch-n should be replaced with
    ;; :fx (mapv (fn [img] [:dispatch [::add-image id img]]) images)
    :dispatch-n (concat
                 (map (fn [img] [::add-image document img]) images)
                 ;; FIXME: the fetch and the navigate should happen
                 ;; only when all images have been uploaded
                 (list
                  [::fetch-images (:id document)]
                  [:common/navigate! :document-images document]))
    }))

(rf/reg-event-db
 ::ack-add-image
 (fn [db [_ document name]]
   (notifications/clear-upload-state db (:id document) name)))

(rf/reg-event-db
 ::ack-failure-image
 (fn [db [_ document name response]]
   (-> db
       (assoc-in [:errors :save] (or (get-in response [:response :status-text])
                                            (get response :status-text)))
       (notifications/clear-upload-state (:id document) name))))

(rf/reg-sub
 ::image-files
 (fn [db _] (-> db :image-files)))

(rf/reg-sub
 ::image-file-names
 :<- [::image-files]
 (fn [files] (->> files (map #(.-name %)) (string/join ", "))))

(rf/reg-event-db
  ::set-image-files
  (fn [db [_ files]] (assoc db :image-files files)))

(defn- image-files []
  (let [get-value (fn [e] (-> e .-target .-files))
        save!     #(rf/dispatch [::set-image-files (toArray %)])
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
        klass (when @(rf/subscribe [::notifications/files-uploading? (:id document)]) "is-loading")
        errors? @(rf/subscribe [::notifications/errors?])]
    (if errors?
      [notifications/error-notification]
      [:div.field.is-grouped
       [image-files]
       [:p.control
        [:button.button.is-success
         {:disabled (or (nil? files) (not authenticated?))
          :class klass
          :on-click (fn [e] (rf/dispatch [::add-all-images document files]))}
         [:span.icon {:aria-hidden true} [:i.mi.mi-backup]]
         [:span (tr [:upload])]]]])))

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
   [:td (last (string/split content #"/"))]
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
        [:table.table.is-striped
         [:thead
          [:tr
           [:th (tr [:image])]
           [:th (tr [:action])]]]
         [:tbody
          (for [{:keys [uuid] :as image} @(rf/subscribe [::images-sorted])]
            ^{:key uuid} [image-row image])]]
        [pagination/pagination [:images] [::fetch-images (:id document)]]])]))
