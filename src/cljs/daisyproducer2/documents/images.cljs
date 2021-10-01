(ns daisyproducer2.documents.image
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

;; see https://www.dotkam.com/2012/11/23/convert-html5-filelist-to-clojure-vector/
(defn toArray [js-col]
  (-> (clj->js [])
      (.-slice)
      (.call js-col)
      (js->clj)))

(defn push-image-uploading-state [db id images]
  (assoc-in db [:loading :images id] (set (map (fn [img] (.-name img)) images))))

(defn pop-image-uploading-state [db id img]
  (update-in db [:loading :images id] disj img))

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
   {:db (push-image-uploading-state db id images)
    ;; FIXME: :dispatch-n should be replaced with
    ;; :fx (mapv (fn [img] [:dispatch [::add-image id img]]) images)
    :dispatch-n (map (fn [img] [::add-image id img]) images)
    }))

(rf/reg-event-db
 ::ack-add-image
 (fn [db [_ id name]]
   (pop-image-uploading-state db id name)))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id name response]]
   (-> db
       (assoc-in [:errors :version] (or (get-in response [:response :status-text])
                                        (get response :status-text)))
       (pop-image-uploading-state id name))))

(rf/reg-sub
  ::images-uploading?
  (fn [db [_ id]]
    (-> db :loading :images (get id) seq)))

(rf/reg-sub
 ::image-files
 (fn [db _] (-> db :image-files)))

(rf/reg-sub
 ::image-file-names
 :<- [::image-files]
 (fn [files] (->> files (map #(.-name %)) (string/join ","))))

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
        [:span.file-label (tr [:choose-file])]]
       [:span.file-name (if files names (tr [:no-file]))]]]]))

(defn upload [id]
  (let [files @(rf/subscribe [::image-files])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        klass (when @(rf/subscribe [::images-uploading? id]) "is-loading")
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

