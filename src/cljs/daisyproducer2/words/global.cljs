(ns daisyproducer2.words.global
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.validation :as validation]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.input-fields :as fields]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(defn- get-search [db] (get-in db [:search :global]))

(rf/reg-event-fx
  ::fetch-words
  (fn [{:keys [db]} [_]]
    (let [search (get-search db)
          offset (pagination/offset db :global)]
      {:db (notifications/set-loading db :global)
       :http-xhrio {:method          :get
                    :uri             "/api/words"
                    :params          (if (str/blank? search)
                                       {:offset offset :limit pagination/page-size}
                                       {:offset offset :limit pagination/page-size :search search})
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-words-success]
                    :on-failure      [::fetch-words-failure]}})))

(rf/reg-event-db
 ::fetch-words-success
 (fn [db [_ words]]
   (let [words (->> words
                    (map #(assoc % :uuid (str (random-uuid)))))
         next? (-> words count (= pagination/page-size))]
     (-> db
         (assoc-in [:words :global] (zipmap (map :uuid words) words))
         (pagination/update-next :global next?)
         (notifications/clear-loading :global)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-words-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-global-words (get response :status-text))
       (notifications/clear-loading :global))))

(rf/reg-event-fx
  ::save-word
  (fn [{:keys [db]} [_ id]]
    (let [word (get-in db [:words :global id])
          cleaned (select-keys word [:untranslated :uncontracted :contracted :type :homograph-disambiguation])]
      {:db (notifications/set-button-state db id :save)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/words")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-save id]
                    :on-failure      [::ack-failure id :save]
                    }})))

(rf/reg-event-fx
  ::delete-word
  (fn [{:keys [db]} [_ id]]
    (let [word (get-in db [:words :global id])
          cleaned (select-keys word [:untranslated :uncontracted :contracted :type :homograph-disambiguation])]
      {:db (notifications/set-button-state db id :delete)
       :http-xhrio {:method          :delete
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/words")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-delete id]
                    :on-failure      [::ack-failure id :delete]
                    }})))

(rf/reg-event-db
  ::ack-save
  (fn [db [_ id]]
    (let [new-id (str (random-uuid))
          new-word (-> db (get-in [:words :global id]) (assoc :uuid new-id))]
      (-> db
          ;; give the saved word a new uuid so that the react component of the
          ;; input fields is re-mounted and its local state is refreshed. The idea
          ;; for this comes from https://stackoverflow.com/a/48451229
          (update-in [:words :global] dissoc id)
          (assoc-in [:words :global new-id] new-word)
          (notifications/clear-button-state id :save)))))

(rf/reg-event-fx
  ::ack-delete
  (fn [{:keys [db]} [_ id]]
    (let [db (-> db
                 (update-in [:words :global] dissoc id)
                 (notifications/clear-button-state id :delete))
          empty? (-> db (get-in [:words :global]) count (< 1))]
      (if empty?
        {:db db :dispatch [::fetch-words]}
        {:db db}))))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id request-type response]]
   (-> db
       (notifications/set-errors request-type (or (get-in response [:response :status-text])
                                                  (get response :status-text)))
       (notifications/clear-button-state id request-type))))

(rf/reg-sub
  ::words
  (fn [db _]
    (->> db :words :global vals)))

(rf/reg-sub
 ::words-sorted
 :<- [::words]
 (fn [words] (->> words (sort-by (juxt :untranslated :type)))))

(rf/reg-sub ::search (fn [db _] (get-search db)))

(rf/reg-event-fx
   ::set-search
   (fn [{:keys [db]} [_ new-search-value]]
     (let [length (count new-search-value)]
       (cond-> {:db (assoc-in db [:search :global] new-search-value)}
         (or (zero? length) (> length 2))
         ;; do not fetch the productions from the server for very small strings,
         ;; unless the string has been reset to the empty string
         (assoc :dispatch-n
                (list
                 ;; when searching for a new word reset the pagination
                 [::pagination/reset :global]
                 [::fetch-words]))))))

(defn words-search []
  (let [get-value (fn [e] (-> e .-target .-value))
        reset!    #(rf/dispatch [::set-search ""])
        save!     #(rf/dispatch [::set-search %])]
    [:div.field
     [:div.control
      [:input.input {:type "text"
                     :placeholder (tr [:search])
                     :aria-label (tr [:search])
                     :value @(rf/subscribe [::search])
                     :on-change #(save! (get-value %))
                     :on-key-down #(when (= (.-which %) 27) (reset!))}]]]))

(defn words-filter []
  [:div.field.is-horizontal
   [:div.field-body
    [words-search]]])

(rf/reg-sub
 ::word
 (fn [db [_ id]]
   (get-in db [:words :global id])))

(rf/reg-sub
 ::valid?
 (fn [db [_ id]]
   (validation/word-valid? (get-in db [:words :global id]))))

(defn buttons [id]
  (let [valid? @(rf/subscribe [::valid? id])
        roles @(rf/subscribe [::auth/user-roles])
        authorized? (auth/intersect? roles #{:admin})]
    [:div.buttons.has-addons
     (if @(rf/subscribe [::notifications/button-loading? id :save])
       [:button.button.is-success.is-loading]
       [:button.button.is-success.has-tooltip-arrow
        {:disabled (not (and valid? authorized?))
         :data-tooltip (tr [:save])
         :aria-label (tr [:save])
         :on-click (fn [e] (rf/dispatch [::save-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-save]]])
     (if @(rf/subscribe [::notifications/button-loading? id :delete])
       [:button.button.is-danger.is-loading]
       [:button.button.is-danger.has-tooltip-arrow
        {:disabled (not authorized?)
         :data-tooltip (tr [:delete])
         :aria-label (tr [:delete])
         :on-click (fn [e] (rf/dispatch [::delete-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-delete]]])]))

(defn word [id]
  (let [{:keys [uuid untranslated type homograph-disambiguation]} @(rf/subscribe [::word id])]
    [:tr
     [:td untranslated]
     [:td [fields/input-field :global uuid :uncontracted validation/braille-valid?]]
     [:td [fields/input-field :global uuid :contracted validation/braille-valid?]]
     [:td {:width "8%"} (get words/type-mapping type (tr [:unknown]))]
     [:td {:width "8%"} homograph-disambiguation]
     [:td {:width "8%"} [buttons uuid]]]))

(defn words-page []
  (let [loading? @(rf/subscribe [::notifications/loading? :global])
        errors? @(rf/subscribe [::notifications/errors?])]
    [:section.section>div.container>div.content
     [:<>
      [words-filter]
      (cond
        errors? [notifications/error-notification]
        loading? [notifications/loading-spinner]
        :else
        [:<>
         [:table.table.is-striped.is-fullwidth
          [:thead
           [:tr
            [:th (tr [:untranslated])]
            [:th (tr [:uncontracted])]
            [:th (tr [:contracted])]
            [:th (tr [:type])]
            [:th (tr [:homograph-disambiguation])]
            [:th (tr [:action])]]]
          [:tbody
           (for [{:keys [uuid]} @(rf/subscribe [::words-sorted])]
             ^{:key uuid} [word uuid])]]
         [pagination/pagination [:words :global] [::fetch-words]]])]]))
