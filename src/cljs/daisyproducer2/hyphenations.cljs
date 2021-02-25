(ns daisyproducer2.hyphenations
  (:require
   [ajax.core :as ajax]
   [daisyproducer2.auth :as auth]
   [re-frame.core :as rf]
   [daisyproducer2.i18n :refer [tr]]
   [daisyproducer2.pagination :as pagination]
   [daisyproducer2.validation :as validation]
   [daisyproducer2.words.notifications :as notifications]
   [clojure.string :as string]))

(rf/reg-event-fx
  ::fetch-hyphenations
  (fn [{:keys [db]} [_]]
    (let [search @(rf/subscribe [::search])
          spelling @(rf/subscribe [::spelling])
          offset (pagination/offset db :hyphenation)]
      {:db (assoc-in db [:loading :hyphenation] true)
       :http-xhrio {:method          :get
                    :uri             "/api/hyphenations"
                    :params          {:spelling spelling
                                      :search (if (nil? search) "" search)
                                      :offset offset
                                      :limit pagination/page-size}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-hyphenations-success]
                    :on-failure      [::fetch-hyphenations-failure :fetch-hyphenations]}})))

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
 (fn [db [_ request-type response]]
   (-> db
       (assoc-in [:errors request-type] (get response :status-text))
       (assoc-in [:loading :hyphenation] false))))

(rf/reg-event-fx
  ::fetch-suggested-hyphenation
  (fn [{:keys [db]} [_]]
    (let [word @(rf/subscribe [::search])
          spelling @(rf/subscribe [::spelling])]
      {:http-xhrio {:method          :get
                    :uri             "/api/hyphenations/suggested"
                    :params          {:spelling spelling
                                      :word word}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::fetch-suggested-hyphenation-success]
                    :on-failure      [::fetch-suggested-hyphenation-failure :fetch-suggested-hyphenation]}})))

(rf/reg-event-db
 ::fetch-suggested-hyphenation-success
 (fn [db [_ {suggested :hyphenation}]]
   (-> db
    (assoc-in [:current :hyphenation :suggested] suggested)
    (assoc-in [:current :hyphenation :corrected] suggested))))

(rf/reg-event-db
 ::fetch-suggested-hyphenation-failure
 (fn [db [_ request-type response]]
   (assoc-in db [:errors request-type] (get response :status-text))))

(rf/reg-event-fx
  ::save-hyphenation
  (fn [{:keys [db]} [_]]
    (let [word @(rf/subscribe [::search])
          hyphenation @(rf/subscribe [::corrected])
          spelling @(rf/subscribe [::spelling])]
      {:db (notifications/set-button-state db :hyphenation :save)
       :http-xhrio {:method          :put
                    :format          (ajax/json-request-format)
                    :headers 	     (auth/auth-header db)
                    :uri             (str "/api/hyphenations")
                    :params          {:word word
                                      :hyphenation hyphenation
                                      :spelling spelling}
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [::ack-save]
                    :on-failure      [::ack-failure :save]
                    }
       :dispatch [::set-search ""]})))

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
                    :on-failure      [::ack-failure :delete]
                    }})))

(rf/reg-event-db
  ::ack-save
  (fn [db [_]]
    (notifications/clear-button-state db :hyphenation :save)))

(rf/reg-event-fx
  ::ack-delete
  (fn [{:keys [db]} [_ id]]
    {:db (-> db
             (update-in [:words :hyphenation] dissoc id)
             (notifications/clear-button-state id :delete))}))

(rf/reg-event-db
 ::ack-failure
 (fn [db [_ request-type response]]
   (-> db
       (assoc-in [:errors request-type] (or (get-in response [:response :status-text])
                                            (get response :status-text)))
       (notifications/clear-button-state :hyphenation request-type))))

(rf/reg-sub
  ::spelling
  (fn [db _]
    (get-in db [:current :spelling] 1)))

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
        {:on-change emit}
        (for [[v s] [[1 (tr [:spelling/new])]
                     [0 (tr [:spelling/old])]]]
          ^{:key v}
          [:option (if (not= current v) {:value v} {:selected "selected" :value v}) s])]]]]))

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

(rf/reg-sub
  ::search
  (fn [db _]
    (get-in db [:search :hyphenation])))

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
                     :value @(rf/subscribe [::search])
                     :on-change #(save! (get-value %))
                     :on-key-down #(when (= (.-which %) 27) (reset!))}]]]))

(defn lookup-button [label href search]
  (let [disabled (when (string/blank? search) "disabled")]
    [:a.button {:href (str href search)
                :disabled disabled
                :target "_blank" }
     label]))

(defn lookup []
  (let [search @(rf/subscribe [::search])
        ]
    [:div.block
     [:label.label (tr [:hyphenation/lookup])]
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
                    (tr [:hyphenation/already-defined]))]
    [:div.field
     [:label.label (tr [:hyphenation/word])]
     [:div.control
      [:input.input {:type "text"
                     :class klass
                     :placeholder (tr [:hyphenation/word])
                     :value @(rf/subscribe [::search])
                     :on-change #(save! (get-value %))
                     :on-key-down #(when (= (.-which %) 27) (reset!))}]]
     (when help-text
       [:p.help.is-danger help-text])]))

(defn suggested-hyphenation []
  (let [suggested @(rf/subscribe [::suggested])]
    [:div.field
     [:label.label (tr [:hyphenation/suggested])]
     [:div.control
      [:input.input {:type "text"
                     :disabled "disabled"
                     :value suggested}]]]))

(rf/reg-sub
  ::corrected
  (fn [db _]
    (get-in db [:current :hyphenation :corrected])))

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
            blank? (string/blank? corrected)
            valid? (or blank? @(rf/subscribe [::valid?]))
            same-as-suggested? (and (not blank?) @(rf/subscribe [::same-as-suggested?]))
            klass (when (or (not valid?) same-as-suggested?) "is-danger")]
        [:div.field
         [:label.label (tr [:hyphenation/corrected])]
         [:div.control
          [:input.input {:type "text"
                         :class klass
                         :value corrected
                         :on-change #(save! (get-value %))}]]
         (when (or (not valid?) same-as-suggested?)
           [:p.help.is-danger
            (if-not valid?
              (tr [:input-not-valid])
              (tr [:hyphenation/same-as-suggested]))])]))))

(defn hyphenation-add-button []
  (let [valid? @(rf/subscribe [::valid?])
        same-as-suggested? @(rf/subscribe [::same-as-suggested?])]
    [:button.button
     {:disabled (when (or (not valid?) same-as-suggested?) "disabled")
      :on-click (fn [e] (rf/dispatch [::save-hyphenation]))}
     (tr [:save])]))

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
        [:table.table.is-striped
         [:thead
          [:tr
           [:th (tr [:hyphenation/word])]
           [:th (tr [:hyphenation/hyphenation])]]]
         [:tbody
          (for [{:keys [word hyphenation]} @(rf/subscribe [::hyphenations-sorted])]
            ^{:key word}
            [:tr
             [:td word]
             [:td hyphenation]])]]
        [pagination/pagination :hyphenation [::fetch-hyphenations]]])
     ]))

(rf/reg-sub
  ::editing
  (fn [db [_ word]]
    (get-in db [:current :hyphenation :editing word])))

(rf/reg-event-db
 ::toggle-editing
 (fn [db [_ word]]
   (update-in db [:current :hyphenation :editing word] not)))

(defn hyphenation-field [word hyphenation]
  (let [editing @(rf/subscribe [::editing word])]
    (if editing
      [:td [:input.input {:type "text" :value hyphenation}]]
      [:td hyphenation])))

(defn button [label handler class icon]
  [:button.button.has-tooltip-arrow
   {:data-tooltip (tr [label])
    :class class
    :on-click #(handler)}
   [:span.icon.is-small
    [:i.mi {:class icon}]]])

(defn buttons [word]
  (let [editing @(rf/subscribe [::editing word])
        save! #(rf/dispatch [::save-hyphenation word])
        delete! #(rf/dispatch [::delete-hyphenation word])
        toggle! #(rf/dispatch [::toggle-editing word])]
    (if editing
      [:div.buttons.has-addons
       [button :save save! "is-success" "mi-done"]
       [button :cancel toggle! "is-danger" "mi-cancel"]]
      [:div.buttons.has-addons
       [button :edit toggle! "is-success" "mi-edit"]
       [button  :delete delete! "is-danger" "mi-delete"]])))

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
        [:table.table.is-striped
         [:thead
          [:tr
           [:th (tr [:hyphenation/word])]
           [:th (tr [:hyphenation/hyphenation])]
           [:th ""]]]
         [:tbody
          (for [{:keys [word hyphenation]} @(rf/subscribe [::hyphenations-sorted])]
            ^{:key word}
            [:tr
             [:td word]
             [hyphenation-field word hyphenation]
             [:td {:width "8%"}
              [buttons word]]])]]
        [pagination/pagination :hyphenation [::fetch-hyphenations]]])]))
