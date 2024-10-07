(ns daisyproducer2.hyphenations
  (:require
   [ajax.core :as ajax]
   [daisyproducer2.auth :as auth]
   [re-frame.core :as rf]
   [daisyproducer2.i18n :refer [tr]]
   [daisyproducer2.pagination :as pagination]
   [daisyproducer2.validation :as validation]
   [daisyproducer2.words.notifications :as notifications]
   [clojure.string :as str]))

(defn- get-search [db] (get-in db [:search :hyphenation]))
(defn- get-spelling [db] (get-in db [:current :spelling] 1))
(defn- get-corrected [db] (get-in db [:current :hyphenation :corrected]))

(rf/reg-event-fx
  ::fetch-hyphenations
  (fn [{:keys [db]} [_]]
    (let [search (get-search db)
          spelling (get-spelling db)
          offset (pagination/offset db :hyphenation)]
      {:db (notifications/set-loading db :hyphenation)
       :http-xhrio {:method          :get
                    :uri             "/api/hyphenations"
                    :params          {:spelling spelling
                                      :search (if (nil? search) "" search)
                                      :offset offset
                                      :limit pagination/page-size}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-hyphenations-success]
                    :on-failure      [::fetch-hyphenations-failure]}})))

(rf/reg-event-db
 ::fetch-hyphenations-success
 (fn [db [_ hyphenations]]
   (let [next? (-> hyphenations count (= pagination/page-size))]
     (-> db
         (assoc-in [:words :hyphenation] (zipmap (map :word hyphenations) hyphenations))
         (pagination/update-next :hyphenation next?)
         (assoc-in [:loading :hyphenation] false)
         ;; clear all button loading states
         (update-in [:loading] dissoc :buttons)))))

(rf/reg-event-db
 ::fetch-hyphenations-failure
 (fn [db [_ response]]
   (-> db
       (notifications/set-errors :fetch-hyphenations (get response :status-text))
       (notifications/clear-loading :hyphenation))))

(rf/reg-event-fx
  ::fetch-suggested-hyphenation
  (fn [{:keys [db]} [_]]
    (let [word (get-search db)
          spelling (get-spelling db)]
      {:http-xhrio {:method          :get
                    :uri             "/api/hyphenations/suggested"
                    :params          {:spelling spelling
                                      :word word}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-suggested-hyphenation-success]
                    :on-failure      [::fetch-suggested-hyphenation-failure]}})))

(rf/reg-event-db
 ::fetch-suggested-hyphenation-success
 (fn [db [_ {suggested :hyphenation}]]
   (-> db
    (assoc-in [:current :hyphenation :suggested] suggested)
    (assoc-in [:current :hyphenation :corrected] suggested))))

(rf/reg-event-db
 ::fetch-suggested-hyphenation-failure
 (fn [db [_ response]]
   (notifications/set-errors db :fetch-suggested-hyphenation (get response :status-text))))

(rf/reg-event-fx
  ::save-hyphenation
  (fn [{:keys [db]} [_ id]]
    (let [word (get-search db)
          hyphenation (if id
                        ;; save an existing hyphenation
                        (get-in db [:words :hyphenation id])
                        ;; insert a new hyphenation
                        {:word word
                         :hyphenation (get-corrected db)
                         :spelling (get-spelling db)})]
      {:db (notifications/set-button-state db (or id word) :save)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/hyphenations")
                    :params          hyphenation
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-save hyphenation (nil? id)]
                    :on-failure      [::ack-failure (or id word) :save]}})))

(rf/reg-event-fx
  ::delete-hyphenation
  (fn [{:keys [db]} [_ id]]
    (let [hyphenation (get-in db [:words :hyphenation id])]
      {:db (notifications/set-button-state db id :delete)
       :http-xhrio {:method          :delete
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/hyphenations")
                    :params          hyphenation
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-delete id]
                    :on-failure      [::ack-failure id :delete]
                    }})))

(rf/reg-event-fx
  ::ack-save
  (fn [{:keys [db]} [_ {id :word :as hyphenation} new?]]
    (let [db (-> db
                 (assoc-in [:words :hyphenation id] hyphenation)
                 (notifications/clear-button-state id :save))]
      (if new?
        {:db db :dispatch [::set-search ""]}
        {:db db}))))

(rf/reg-event-db
  ::ack-delete
  (fn [db [_ id]]
    (-> db
        (update-in [:words :hyphenation] dissoc id)
        (notifications/clear-button-state id :delete))))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ id request-type response]]
   (-> db
       (notifications/set-errors request-type (or (get-in response [:response :status-text])
                                                  (get response :status-text)))
       (notifications/clear-button-state id request-type))))

(rf/reg-sub ::spelling (fn [db _] (get-spelling db)))

(rf/reg-event-fx
  ::set-spelling
  (fn [{:keys [db]} [_ spelling]]
    {:db (assoc-in db [:current :spelling] (js/parseInt spelling))
     :dispatch-n
     (list
      ;; when changing the spelling reset the pagination
      [::pagination/reset :hyphenation]
      [::fetch-hyphenations])}))

(defn spelling-selector []
  (let [current @(rf/subscribe [::spelling])
        getvalue (fn [e] (-> e .-target .-value))
        emit     (fn [e] (rf/dispatch [::set-spelling (getvalue e)]))]
    [:div.field
     [:div.control
      [:div.select.is-fullwidth
       [:select
        {:value current
         :on-change emit}
        (for [[v s] [[1 (tr [:new-spelling])]
                     [0 (tr [:old-spelling])]]]
          ^{:key v}
          [:option {:value v} s])]]]]))

(rf/reg-sub
  ::hyphenations
  (fn [db _]
    (get-in db [:words :hyphenation])))

(rf/reg-sub
 ::hyphenations-sorted
 :<- [::hyphenations]
 (fn [hyphenations] (->> hyphenations vals (sort-by :word))))

(rf/reg-sub
 ::already-defined?
 :<- [::hyphenations]
 :<- [::search]
 (fn [[hyphenations search]] (contains? hyphenations search)))

(rf/reg-sub
  ::suggested
  (fn [db _]
    (get-in db [:current :hyphenation :suggested] "")))

(rf/reg-sub ::search (fn [db _] (get-search db)))

(rf/reg-event-fx
   ::set-search
   (fn [{:keys [db]} [_ new-search-value]]
     {:db (assoc-in db [:search :hyphenation] new-search-value)
      :dispatch-n
      (list
       ;; when searching for a new hyphenation reset the pagination
       [::pagination/reset :hyphenation]
       [::fetch-suggested-hyphenation]
       [::fetch-hyphenations])}))

(defn search []
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

(defn lookup-button [label href search]
  (let [disabled (when (str/blank? search) "disabled")]
    [:a.button {:href (str href search)
                :disabled disabled
                :target "_blank" }
     label]))

(defn lookup []
  (let [search @(rf/subscribe [::search])
        ]
    [:div.block
     [:label.label (tr [:lookup-hyphenation])]
     [:div.buttons
      [lookup-button "Duden" "http://www.duden.de/suchen/dudenonline/" search]
      [lookup-button "TU Chemnitz" "http://dict.tu-chemnitz.de/?query=" search]
      [lookup-button "PONS" "http://de.pons.eu/dict/search/results/?l=dede&q=" search]]]))

(defn word []
  (let [get-value (fn [e] (-> e .-target .-value))
        reset! #(rf/dispatch [::set-search ""])
        save! #(rf/dispatch [::set-search %])
        already-defined? @(rf/subscribe [::already-defined?])
        klass (when already-defined? "is-danger")
        help-text (when already-defined?
                    (tr [:already-defined-hyphenation]))]
    [:div.field
     [:label.label
      {:for "hyphenation-word"}
      (tr [:word])]
     [:div.control
      [:input.input {:type "text"
                     :id "hyphenation-word"
                     :class klass
                     :placeholder (tr [:word])
                     :aria-label (tr [:word])
                     :value @(rf/subscribe [::search])
                     :on-change #(save! (get-value %))
                     :on-key-down #(when (= (.-which %) 27) (reset!))}]]
     (when help-text
       [:p.help.is-danger help-text])]))

(defn suggested-hyphenation []
  (let [suggested @(rf/subscribe [::suggested])]
    [:div.field
     [:label.label
      {:for "hyphenation-suggested"}
      (tr [:suggested-hyphenation])]
     [:div.control
      [:input.input {:type "text"
                     :id "hyphenation-suggested"
                     :disabled "disabled"
                     :value suggested}]]]))

(rf/reg-sub ::corrected (fn [db _] (get-corrected db)))

(rf/reg-event-db
 ::set-corrected
 (fn [db [_ value]]
   (assoc-in db [:current :hyphenation :corrected] value)))

(rf/reg-sub
 ::same-as-suggested?
 :<- [::suggested]
 :<- [::corrected]
 (fn [[suggested corrected]] (= suggested corrected)))

(rf/reg-sub
 ::valid?
 :<- [::search]
 :<- [::corrected]
 (fn [[search corrected]] (validation/hyphenation-valid? corrected search)))

(defn corrected-hyphenation []
  (let [get-value (fn [e] (-> e .-target .-value))
        save! #(rf/dispatch [::set-corrected %])]
    (fn []
      (let [corrected @(rf/subscribe [::corrected])
            blank? (str/blank? corrected)
            valid? (or blank? @(rf/subscribe [::valid?]))
            same-as-suggested? (and (not blank?) @(rf/subscribe [::same-as-suggested?]))
            klass (when (or (not valid?) same-as-suggested?) "is-danger")]
        [:div.field
         [:label.label
          {:for "hyphenation-corrected"}
          (tr [:corrected-hyphenation])]
         [:div.control
          [:input.input {:type "text"
                         :id "hyphenation-corrected"
                         :class klass
                         :value corrected
                         :on-change #(save! (get-value %))}]]
         (when (or (not valid?) same-as-suggested?)
           [:p.help.is-danger
            (if-not valid?
              (tr [:input-not-valid])
              (tr [:same-as-suggested-hyphenation]))])]))))

(defn hyphenation-add-button []
  (let [valid? @(rf/subscribe [::valid?])
        same-as-suggested? @(rf/subscribe [::same-as-suggested?])
        word @(rf/subscribe [::search])
        klass (when  @(rf/subscribe [::notifications/button-loading? word :save])
                "is-loading")]
    [:div.buttons.has-addons
     [:button.button.is-success
      {:disabled (when (or (not valid?) same-as-suggested?) "disabled")
       :class klass
       :on-click (fn [e] (rf/dispatch [::save-hyphenation]))}
      [:span.icon {:aria-hidden true} [:i.mi.mi-save]]
      [:span (tr [:save])]]]))

(defn hyphenation-form []
  (let [already-defined? @(rf/subscribe [::already-defined?])]
    [:div.block
     [word]
     (when-not already-defined?
       [:<>
        [suggested-hyphenation]   
        [corrected-hyphenation]
        [hyphenation-add-button]])]))

(defn tab-link [uri title page on-click]
  (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
    [:li.is-active [:a title]]
    [:li [:a {:href uri :on-click on-click} title]]))

(defn tabs []
  [:div.block
   [:div.tabs.is-boxed
    [:ul
     [tab-link "#/hyphenations" (tr [:insert]) :hyphenations]
     [tab-link "#/hyphenations/edit" (tr [:edit]) :hyphenations-edit]]]])

(defn add-page []
  (let [spelling @(rf/subscribe [::spelling])
        loading? @(rf/subscribe [::notifications/loading? :hyphenation])
        errors? @(rf/subscribe [::notifications/errors?])]
    [:section.section>div.container>div.content
     [spelling-selector]
     [tabs]
     [hyphenation-form]
     [lookup]
     (cond
       errors? [notifications/error-notification]
       loading? [notifications/loading-spinner]
       :else
       [:<>
        [:table.table.is-striped.is-fullwidth
         [:thead
          [:tr
           [:th (tr [:word])]
           [:th (tr [:hyphenation])]]]
         [:tbody
          (for [{:keys [word hyphenation]} @(rf/subscribe [::hyphenations-sorted])]
            ^{:key word}
            [:tr
             [:td word]
             [:td hyphenation]])]]
        [pagination/pagination [:words :hyphenation] [::fetch-hyphenations]]])
     ]))

(rf/reg-sub
 ::hyphenation
 (fn [db [_ id]]
   (get-in db [:words :hyphenation id :hyphenation])))

(rf/reg-sub
 ::hyphenation-valid?
 (fn [db [_ id]]
   (let [{:keys [word hyphenation]} (get-in db [:words :hyphenation id])]
     (validation/hyphenation-valid? hyphenation word))))

(rf/reg-event-db
 ::set-hyphenation
 (fn [db [_ id value]]
   (assoc-in db [:words :hyphenation id :hyphenation] value)))

(defn hyphenation-field [word hyphenation]
  (let [initial-value hyphenation
        get-value (fn [e] (-> e .-target .-value))
        reset! #(rf/dispatch [::set-hyphenation word initial-value])
        save! #(rf/dispatch [::set-hyphenation word %])]
    (fn []
      (let [value @(rf/subscribe [::hyphenation word])
            valid? (validation/hyphenation-valid? value word)
            changed? (not= initial-value value)
            klass (list (cond (not valid?) "is-danger"
                              changed? "is-warning"))]
        [:div.field
         [:input.input {:type "text"
                        :class klass
                        :aria-label (tr [:corrected-hyphenation])
                        :value value
                        :on-change #(save! (get-value %))
                        :on-key-down #(when (= (.-which %) 27) (reset!))}]
         (when-not valid?
           [:p.help.is-danger (tr [:input-not-valid])])]))))

(defn buttons [id]
  (let [valid? @(rf/subscribe [::hyphenation-valid? id])
        authenticated? @(rf/subscribe [::auth/authenticated?])]
    [:div.buttons.has-addons
     (if @(rf/subscribe [::notifications/button-loading? id :save])
       [:button.button.is-success.is-loading]
       [:button.button.is-success.has-tooltip-arrow
        {:disabled (not (and valid? authenticated?))
         :data-tooltip (tr [:save])
         :aria-label (tr [:save])
         :on-click #(rf/dispatch [::save-hyphenation id])}
        [:span.icon {:aria-hidden true} [:i.mi.mi-save]]])
     (if @(rf/subscribe [::notifications/button-loading? id :delete])
       [:button.button.is-danger.is-loading]
       [:button.button.is-danger.has-tooltip-arrow
        {:disabled (not authenticated?)
         :data-tooltip (tr [:delete])
         :aria-label (tr [:delete])
         :on-click #(rf/dispatch [::delete-hyphenation id])}
        [:span.icon {:aria-hidden true} [:i.mi.mi-delete]]])]))

(defn edit-page []
  (let [spelling @(rf/subscribe [::spelling])
        loading? @(rf/subscribe [::notifications/loading? :hyphenation])
        errors? @(rf/subscribe [::notifications/errors?])]
    [:section.section>div.container>div.content
     [spelling-selector]
     [tabs]
     [search]
     (cond
       errors? [notifications/error-notification]
       loading? [notifications/loading-spinner]
       :else
       [:<>
        [:table.table.is-striped.is-fullwidth
         [:thead
          [:tr
           [:th (tr [:word])]
           [:th (tr [:hyphenation])]
           [:th ""]]]
         [:tbody
          (for [{:keys [word hyphenation]} @(rf/subscribe [::hyphenations-sorted])]
            ^{:key word}
            [:tr
             [:td word]
             [:td [hyphenation-field word hyphenation]]
             [:td {:width "8%"}
              [buttons word]]])]]
        [pagination/pagination [:words :hyphenation] [::fetch-hyphenations]]])]))
