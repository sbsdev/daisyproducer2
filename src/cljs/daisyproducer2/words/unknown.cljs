(ns daisyproducer2.words.unknown
  (:require [ajax.core :as ajax]
            [ajax.protocols :as pr]
            [daisyproducer2.auth :as auth]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.pagination :as pagination]
            [daisyproducer2.submit-all :as submit-all]
            [daisyproducer2.validation :as validation]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.input-fields :as fields]
            [daisyproducer2.words.notifications :as notifications]
            [daisyproducer2.words.totals :as totals]
            [re-frame.core :as rf]))

(rf/reg-event-fx
  ::fetch-words
  (fn [{:keys [db]} [_ id]]
    (let [grade (grade/get-grade db)
          offset (pagination/offset db :unknown)]
      {:db (notifications/set-loading db :unknown)
       :http-xhrio {:method          :get
                    :uri             (str "/api/documents/" id "/unknown-words")
                    :params          {:grade grade
                                      :offset offset
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
         (assoc-in [:words :unknown] (zipmap (map :uuid words) words))
         (pagination/update-next :unknown next?)
         (notifications/clear-loading :unknown)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-words-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-words (get response :status-text))
       (notifications/clear-loading :unknown))))

(rf/reg-event-fx
  ::save-word
  (fn [{:keys [db]} [_ id]]
    (let [word (get-in db [:words :unknown id])
          cleaned (select-keys word [:untranslated :uncontracted :contracted :type :homograph-disambiguation
                                     :document-id :islocal :hyphenated :spelling])
          document-id (:document-id word)]
      {:db (notifications/set-button-state db id :save)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" document-id "/words")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-save id document-id]
                    :on-failure      [::ack-failure id :save]}})))

(rf/reg-event-fx
  ::save-all-words
  (fn [{:keys [db]} _]
    (let [ids (keys (get-in db [:words :unknown]))]
      {:dispatch-n (map (fn [id] [::save-word id]) ids)})))

(rf/reg-event-fx
  ::ack-save
  (fn [{:keys [db]} [_ id document-id]]
    (let [db (-> db
                 (update-in [:words :unknown] dissoc id)
                 (notifications/clear-button-state id :save))
          empty? (-> db (get-in [:words :unknown]) count (< 1))]
      (if empty?
        {:db db :dispatch-n [[::fetch-words document-id]
                             [::decrement-words-total id]
                             [::totals/increment-local-words]]}
        {:db db :dispatch-n [[::decrement-words-total id]
                             [::totals/increment-local-words]]}))))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id request-type response]]
   (-> db
       (notifications/set-errors request-type (or (get-in response [:response :status-text])
                                                  (get response :status-text)))
       (notifications/clear-button-state id request-type))))

(rf/reg-event-fx
  ::ignore-word
  (fn [{:keys [db]} [_ uuid]]
    (let [word (get-in db [:words :unknown uuid])
          cleaned (-> word
                      (select-keys [:untranslated :type :homograph-disambiguation
                                    :document-id :islocal :isignored])
                      (assoc :isignored true))
          document-id (:document-id word)]
      {:db (notifications/set-button-state db uuid :ignore)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/documents/" document-id "/unknown-words")
                    :params          cleaned
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-ignore uuid document-id]
                    :on-failure      [::ack-failure uuid :ignore]
                    }})))

(rf/reg-event-fx
  ::ack-ignore
  (fn [{:keys [db]} [_ uuid document-id]]
    (let [db (-> db
                 (update-in [:words :unknown] dissoc uuid)
                 (notifications/clear-button-state uuid :ignore))
          empty? (-> db (get-in [:words :unknown]) count (< 1))]
      (if empty?
        {:db db :dispatch [::fetch-words document-id]}
        {:db db}))))

(rf/reg-sub
 ::words
 (fn [db _]
   (->> db :words :unknown vals)))

(rf/reg-sub
 ::words-sorted
 :<- [::words]
 (fn [words] (->> words (sort-by (juxt :document-id :isignored :untranslated :type)))))

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
   (get-in db [:words :unknown id])))

(rf/reg-sub
 ::valid?
 (fn [db [_ id]]
   (validation/word-valid? (get-in db [:words :unknown id]))))

(rf/reg-sub
 ::ignored?
 (fn [db [_ id]]
   (get-in db [:words :unknown id :isignored])))

(rf/reg-event-fx
  ::fetch-words-total
  (fn [{:keys [db]} [_ id]]
    (let [grade (grade/get-grade db)]
      {:http-xhrio {:method          :head
                    :format          (ajax/url-request-format)
                    :uri             (str "/api/documents/" id "/unknown-words")
                    :url-params      {:grade grade}
                    ;; we are only interested in the headers of the response, not in the response
                    ;; body itself. So we have to specify a special response format, see
                    ;; https://github.com/JulianBirch/cljs-ajax/blob/master/docs/formats.md#non-standard-formats
                    :response-format {:read (fn [resp] (pr/-get-response-header resp "x-result-count"))
                                      :description "X-Result-Count header"}
                    :on-success      [::fetch-words-total-success]
                    :on-failure      [::fetch-words-total-failure :fetch-words]}})))

(rf/reg-event-db
 ::fetch-words-total-success
 (fn [db [_ total]] (assoc-in db [:totals :unknown] (parse-long total))))

(rf/reg-event-db
 ::fetch-words-total-failure
 (fn [db [_ request-type response]]
   (notifications/set-errors db request-type (get response :status-text))))

(rf/reg-event-db
 ::decrement-words-total
 (fn [db [_]] (update-in db [:totals :unknown] dec)))

(rf/reg-sub
 ::words-total
 (fn [db _] (get-in db [:totals :unknown] 0)))

(defn buttons [id]
  (let [valid? @(rf/subscribe [::valid? id])
        authenticated? @(rf/subscribe [::auth/authenticated?])
        ignored? @(rf/subscribe [::ignored? id])]
    [:div.buttons.has-addons
     (if @(rf/subscribe [::notifications/button-loading? id :save])
       [:button.button.is-success.is-loading]
       [:button.button.is-success.has-tooltip-arrow
        {:disabled (not (and valid? authenticated?))
         :data-tooltip (tr [:approve])
         :aria-label (tr [:approve])
         :on-click (fn [e] (rf/dispatch [::save-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-done]]])
     (if @(rf/subscribe [::notifications/button-loading? id :ignore])
       [:button.button.is-success.is-loading]
       [:button.button.is-danger.has-tooltip-arrow
        {:disabled (or ignored? (not authenticated?))
         :data-tooltip (tr [:ignore])
         :aria-label (tr [:ignore])
         :on-click (fn [e] (rf/dispatch [::ignore-word id]))}
        [:span.icon {:aria-hidden true} [:i.mi.mi-cancel]]])]))

(defn word [id]
  (let [grade @(rf/subscribe [::grade/grade])
        {:keys [uuid untranslated uncontracted contracted type homograph-disambiguation
                hyphenated invalid-hyphenated]} @(rf/subscribe [::word id])
        ;; if we have neither the uncontracted nor the contracted field,
        ;; most likely the braille translation failed
        valid? (or uncontracted contracted)]
    [:tr
     [:td untranslated]
     (when (#{0 1} grade)
       (if uncontracted
         [:td [fields/input-field :unknown uuid :uncontracted validation/braille-valid?]]
         [:td [fields/invalid-field]]))
     (when (#{0 2} grade)
       (if contracted
         [:td [fields/input-field :unknown uuid :contracted validation/braille-valid?]]
         [:td [fields/invalid-field]]))
     [:td (if hyphenated
            [fields/input-field :unknown uuid :hyphenated #(validation/hyphenation-valid? % untranslated)]
            [fields/disabled-field invalid-hyphenated])]
     [:td {:width "8%"} (get words/type-mapping type (tr [:unknown]))]
     [:td {:width "8%"} homograph-disambiguation]
     [:td (when valid? [fields/local-field :unknown uuid])]
     [:td {:width "8%"} (when valid? [buttons uuid])]]))

(defn unknown-words []
  (let [words @(rf/subscribe [::words-sorted])
        document @(rf/subscribe [:daisyproducer2.documents.document/current])
        spelling (:spelling (first words))
        grade @(rf/subscribe [::grade/grade])
        loading? @(rf/subscribe [::notifications/loading? :unknown])
        errors? @(rf/subscribe [::notifications/errors?])]
    (cond
      errors? [notifications/error-notification]
      loading? [notifications/loading-spinner]
      :else
      [:<>
       [:table.table.is-striped.is-fullwidth
        [:thead
         [:tr
          [:th (tr [:untranslated])]
          (when (#{0 1} grade) [:th (tr [:uncontracted])])
          (when (#{0 2} grade) [:th (tr [:contracted])])
          [:th (tr [:hyphenated-with-spelling] [(words/spelling-string spelling)])]
          [:th (tr [:type])]
          [:th (tr [:homograph-disambiguation])]
          [:th (tr [:local])]
          [:th (tr [:action])]]]
        [:tbody
         (for [{:keys [uuid]} words]
           ^{:key uuid}
           [word uuid])]]
       [submit-all/buttons (tr [:approve-all]) [::words-valid?] [::has-words?] [::save-all-words]]
       [pagination/pagination [:words :unknown] [::fetch-words (:id document)]]])))
