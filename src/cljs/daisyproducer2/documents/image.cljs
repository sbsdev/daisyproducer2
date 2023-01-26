(ns daisyproducer2.documents.image
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-images
  (fn [{:keys [db]} [_ document-id]]
    (let [offset (pagination/offset db :images)]
      {:db (assoc-in db [:loading :images] true)
       :http-xhrio (as-transit
                    {:method          :get
                     :uri             (str "/api/documents/" document-id "/images")
                     :params          {:offset offset
                                       :limit pagination/page-size}
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
 (fn [images] (->> images (sort-by :title))))

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
  (fn [{:keys [db]} [_ id js-file-value]]
    (let [name (.-name js-file-value)
          form-data (doto (js/FormData.)
                      (.append "file" js-file-value name))]
      {:http-xhrio {:method          :post
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" id "/images")
                    :body            form-data
                    :response-format (ajax/raw-response-format)
                    :on-success      [::ack-add-image id name]
                    :on-failure      [::ack-failure id name]
                    }})))

(rf/reg-event-fx
 ::add-all-images
 (fn [{:keys [db]} [_ id images]]
   {:db (notifications/set-upload-state db id (set (extract-filenames images)))
    ;; FIXME: :dispatch-n should be replaced with
    ;; :fx (mapv (fn [img] [:dispatch [::add-image id img]]) images)
    :dispatch-n (map (fn [img] [::add-image id img]) images)
    }))

(rf/reg-event-db
 ::ack-add-image
 (fn [db [_ id name]]
   (notifications/clear-upload-state db id name)))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id name response]]
   (-> db
       (assoc-in [:errors :version] (or (get-in response [:response :status-text])
                                        (get response :status-text)))
       (notifications/clear-upload-state id name))))

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

(defn upload [id]
  (let [files @(rf/subscribe [::image-files])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        klass (when @(rf/subscribe [::notifications/files-uploading? id]) "is-loading")
        errors? @(rf/subscribe [::notifications/errors?])]
    (if errors?
      [notifications/error-notification]
      [:div.field.is-grouped
       [image-files]
       [:p.control
        [:button.button.is-success
         {:disabled (or (nil? files) (not authenticated?))
          :class klass
          :on-click (fn [e] (rf/dispatch [::add-all-images id files]))}
         [:span.icon {:aria-hidden true} [:i.mi.mi-backup]]
         [:span (tr [:upload])]]]])))

(defn- image-row [{:keys [content]}]
  [:tr
   [:td (last (string/split content #"/"))]
   [:td]])

(defn images [id]
  (if-let [errors? @(rf/subscribe [::notifications/errors?])]
    [notifications/error-notification]
    [:table.table.is-striped
     [:thead
      [:tr
       [:th (tr [:image])]
       [:th (tr [:action])]]]
     [:tbody
      (for [{:keys [uuid] :as image} @(rf/subscribe [::images-sorted])]
        ^{:key uuid} [image-row image])]]))
