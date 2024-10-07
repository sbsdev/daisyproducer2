(ns daisyproducer2.words.confirm
  (:require [ajax.core :as ajax]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.submit-all :as submit-all]
            [daisyproducer2.validation :as validation]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.input-fields :as fields]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-words
  (fn [{:keys [db]} [_]]
    (let [offset (pagination/offset db :confirm)]
      {:db (notifications/set-loading db :confirm)
       :http-xhrio {:method          :get
                    :uri             "/api/confirmable"
                    :params          {:offset offset
                                      :limit pagination/page-size}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-words-success]
                    :on-failure      [::fetch-words-failure]}})))

(rf/reg-event-db
 ::fetch-words-success
 (fn [db [_ words]]
   (let [words (map #(assoc % :uuid (str (random-uuid))) words)
         next? (-> words count (= pagination/page-size))]
     (-> db
         (assoc-in [:words :confirm] (zipmap (map :uuid words) words))
         (pagination/update-next :confirm next?)
         (notifications/clear-loading :confirm)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-words-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-confirm-words (get response :status-text))
       (notifications/clear-loading :confirm))))

(rf/reg-event-fx
  ::save-word
  (fn [{:keys [db]} [_ id]]
    (let [word (get-in db [:words :confirm id])
          cleaned (select-keys word [:untranslated :uncontracted :contracted :type :homograph-disambiguation
                                     :document-id :hyphenated :spelling :islocal])]
      {:db (notifications/set-button-state db id :save)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/confirmable")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-save id]
                    :on-failure      [::ack-failure id :save]
                    }})))

(rf/reg-event-fx
  ::save-all-words
  (fn [{:keys [db]} _]
    (let [ids (keys (get-in db [:words :confirm]))]
      {:dispatch-n (map (fn [id] [::save-word id]) ids)})))

(rf/reg-event-fx
  ::delete-word
  (fn [{:keys [db]} [_ id]]
    (let [word (get-in db [:words :confirm id])
          cleaned (select-keys word [:untranslated :uncontracted :contracted :type :homograph-disambiguation
                                     :document-id :hyphenated :spelling])
          document-id (:document-id word)]
      {:db (notifications/set-button-state db id :delete)
       :http-xhrio {:method          :delete
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" document-id "/words")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-delete id]
                    :on-failure      [::ack-failure id :delete]
                    }})))

(rf/reg-event-fx
  ::ack-save
  (fn [{:keys [db]} [_ id]]
    (let [db (-> db
                 (update-in [:words :confirm] dissoc id)
                 (notifications/clear-button-state id :save))
          empty? (-> db (get-in [:words :confirm]) count (< 1))]
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

(rf/reg-event-fx
  ::ack-delete
  (fn [{:keys [db]} [_ id]]
    (let [db (-> db
                 (update-in [:words :confirm] dissoc id)
                 (notifications/clear-button-state id :delete))
          empty? (-> db (get-in [:words :confirm]) count (< 1))]
      (if empty?
        {:db db :dispatch [::fetch-words]}
        {:db db}))))

(rf/reg-sub
  ::words
  (fn [db _]
    (-> db :words :confirm vals)))

(rf/reg-sub
 ::words-sorted
 :<- [::words]
 (fn [words] (->> words (sort-by (juxt :document-id :untranslated :type)))))

(rf/reg-sub
 ::has-words?
 :<- [::words]
 (fn [words] (->> words seq some?)))

(rf/reg-sub
 ::words-valid?
 :<- [::words]
 (fn [words] (every? validation/word-valid? words)))

(rf/reg-sub
 ::word
 (fn [db [_ id]]
   (get-in db [:words :confirm id])))

(rf/reg-sub
 ::valid?
 (fn [db [_ id]]
   (validation/word-valid? (get-in db [:words :confirm id]))))

(defn buttons [id]
  (let [valid? @(rf/subscribe [::valid? id])
        authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:div.buttons.has-addons
     (if @(rf/subscribe [::notifications/button-loading? id :save])
       [:button.button.is-success.is-loading]
       [:button.button.is-success.has-tooltip-arrow
        {:disabled (not (and valid? authenticated?))
         :data-tooltip (tr [:approve])
         :aria-label (tr [:approve])
         :on-click (fn [e] (rf/dispatch [::save-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-done]]])
     (if @(rf/subscribe [::notifications/button-loading? id :delete])
       [:button.button.is-danger.is-loading]
       [:button.button.is-danger.has-tooltip-arrow
        {:disabled (not authenticated?)
         :data-tooltip (tr [:delete])
         :aria-label (tr [:delete])
         :on-click (fn [e] (rf/dispatch [::delete-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-delete]]])]))

(defn type-field [id]
  (let [type @(rf/subscribe [::fields/word-field :confirm id :type])
        set-type-fn (fn [type]
                      (fn [e] (rf/dispatch [::fields/set-word-field :confirm id :type type])))]
    (case type
      0 nil
      (1 2)
      [:div.select
       [:select {:value type}
        [:option {:value 2 :on-click (set-type-fn 2)} (tr [:type-name])]
        [:option {:value 1 :on-click (set-type-fn 1)} (tr [:type-name-hoffmann])]]]
      (3 4)
      [:div.select
       [:select {:value type}
        [:option {:value 4 :on-click (set-type-fn 4)} (tr [:type-place])]
        [:option {:value 3 :on-click (set-type-fn 3)} (tr [:type-place-langenthal])]]]
      5 (tr [:type-homograph])
      :else (tr [:unknown]))))

(defn word [id]
  (let [{:keys [uuid untranslated type homograph-disambiguation hyphenated spelling document-title]} @(rf/subscribe [::word id])]
    [:tr
     [:td untranslated]
     [:td [fields/input-field :confirm uuid :uncontracted validation/braille-valid?]]
     [:td [fields/input-field :confirm uuid :contracted validation/braille-valid?]]
     [:td (when hyphenated
            [fields/input-field :confirm uuid :hyphenated #(validation/hyphenation-valid? % untranslated)])]
     (let [spelling-string (words/spelling-brief-string spelling)]
       [:td [:abbr {:title spelling-string} (subs spelling-string 0 1)]])
     [:td [type-field uuid]]
     [:td homograph-disambiguation]
     [:td
      ;; for screen readers we just (additionally) show the whole document title
      [:span.is-sr-only document-title]
      [:abbr {:title document-title } (str (subs document-title 0 3) "...")]]
     [:td [fields/local-field :confirm uuid]]
     [:td {:width "8%"} [buttons uuid]]]))

(defn words-page []
  (let [loading? @(rf/subscribe [::notifications/loading? :confirm])
        errors? @(rf/subscribe [::notifications/errors?])]
    [:section.section>div.container>div.content
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
           [:th (tr [:hyphenated])]
           [:th [:abbr {:title (tr [:spelling])} (subs (tr [:spelling]) 0 1)]]
           [:th [:abbr {:title (tr [:type])} (subs (tr [:type]) 0 1)]]
           [:th [:abbr {:title (tr [:homograph-disambiguation])} (subs (tr [:homograph-disambiguation]) 0 1)]]
           [:th (tr [:book])]
           [:th [:abbr {:title (tr [:local])} (subs (tr [:local]) 0 1)]]
           [:th (tr [:action])]]]
         [:tbody
          (for [{:keys [uuid]} @(rf/subscribe [::words-sorted])]
            ^{:key uuid} [word uuid])]]
        [submit-all/buttons (tr [:approve-all]) [::words-valid?] [::has-words?] [::save-all-words]]
        [pagination/pagination [:words :confirm] [::fetch-words]]])]))
