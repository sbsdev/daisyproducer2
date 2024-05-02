(ns daisyproducer2.documents.preview.forms
  (:require [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.ajax :refer [as-transit]]
            [fork.re-frame :as fork]
            [re-frame.core :as rf]))

(rf/reg-event-fx
 ::success
 (fn [{db :db} [_ path format response]]
   (let [_ (js/window.open (:location response) "_blank")]
     {:db (-> db
              (fork/set-submitting path false))})))

(rf/reg-event-fx
 ::failure
 (fn [{db :db} [_ path format response]]
   {:db (-> db
            (fork/set-submitting path false)
            (fork/set-server-message path (str "Fetching " (name format) " failed!")))}))

(rf/reg-event-fx
 ::submit-handler
 (fn [{db :db} [_ format id {:keys [values path]}]]
   {:db (fork/set-submitting db path true)
    :http-xhrio
    (as-transit
     {:method :get
      :uri (str "/api/documents/" id "/preview/" (name format))
      :params values
      :timeout 2000
      :on-success [::success path format]
      :on-failure [::failure path format]})}))

(rf/reg-event-db
 ::ack-error
 (fn [db [_ path]]
   (fork/set-server-message db path false)))

(defn error-notification [message path]
  [:div.notification.is-danger
   [:button.delete
    {:on-click (fn [e] (rf/dispatch [::ack-error path]))}]
   [:p [:strong message]]])

(defn input
  [{:keys [values handle-change handle-blur disabled?]}
   {:keys [label placeholder name type class]}]
  [:div.field.is-horizontal
   {:class class}
   [:div.field-label
    [:label.label label]]
   [:div.field-body
    [:div.field
     [:div.control
      [:input.input
       {:name name
        :placeholder placeholder
        :type type
        :value (values name "")
        :disabled (disabled? name)
        :on-change handle-change
        :on-blur handle-blur}]]]]])

(defn checkbox
  [{:keys [values handle-change handle-blur disabled?]}
   {:keys [name class text]}]
  [:div.field.is-horizontal
   {:class class}
   [:div.field-label
    [:label.label text]]
   [:div.field-body
    [:div.field
     [:div.control
      [:label.checkbox
       [:input
        {:name name
         :type "checkbox"
         :checked (values name false)
         :disabled (disabled? name)
         :on-change handle-change
         :on-blur handle-blur}]
       #_(str " " text)]]]]])

(defn dropdown
  [{:keys [values handle-change handle-blur disabled?]}
   {:keys [label name options class]}]
  [:div.field.is-horizontal
   {:class class}
   [:div.field-label
    [:label.label label]]
   [:div.field-body
    [:div.field
     [:div.control
      [:div.select
       [:select
        {:name name
         :value (values name "")
         :on-change handle-change
         :on-blur handle-blur
         :disabled (disabled? name)}
        (for [option options]
          ^{:key (ffirst option)}
          [:option
           {:value (ffirst option)}
           (first (vals option))])]]]]]])

(defn submit-button
  [{:keys [values handle-change handle-blur disabled? submitting?]}
   {:keys [label name options class]}]
  [:div.field.is-horizontal
   {:class class}
   [:div.field-label]
   [:div.field-body
    [:div.field
     [:div.control
      [:button.button.is-primary
       {:type "submit"
        :class (when submitting? "is-loading")
        :disabled (disabled? name)}
       [:span label]
       [:span.icon {:aria-hidden true}
        [:i.material-icons "download"]]]]]]])

(defn braille [{id :id}]
  [fork/form {:initial-values {:cells-per-line 28 :lines-per-page 28
                               :contraction 2 :hyphenation false
                               :toc-level 0 :footer-level 0
                               :include-macros true :show-original-page-numbers true
                               :show-v-forms true :downshift-ordinals true
                               :enable-capitalization false
                               :detailed-accented-chars :swiss :footnote-placement :standard}
              :path [:form :braille]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit-handler :braille id %])
              :keywordize-keys true
              }
   (fn [{:keys [path
                form-id
                submitting?
		on-submit-server-message
                handle-submit] :as props}]
     (if on-submit-server-message
       [error-notification on-submit-server-message path]
       [:form {:id form-id :on-submit handle-submit}
        (input props {:name :cells-per-line :label (tr [:forms/cells-per-line]) :type "text"})
        (input props {:name :lines-per-page :label (tr [:forms/lines-per-page]) :type "text"})
        (dropdown props {:name :contraction :label (tr [:forms/contraction])
                               :options [{0 (tr [:grade/g0])} {1 (tr [:grade/g1])} {2 (tr [:grade/g2])}]})
        (checkbox props {:name :hyphenation :text (tr [:forms/hyphenation])})
        (dropdown props {:name :toc-level :label (tr [:forms/toc-level])
                               :options [{0 0} {1 1} {2 2} {3 3} {4 4} {5 5} {6 6}]})
        (dropdown props {:name :footer-level :label (tr [:forms/footer-level])
                               :options [{0 0} {1 1} {2 2} {3 3} {4 4} {5 5} {6 6}]})
        (checkbox props {:name :include-macros :text (tr [:forms/include-macros])})
        (checkbox props {:name :show-original-page-numbers :text (tr [:forms/show-original-page-numbers])})
        (checkbox props {:name :show-v-forms :text (tr [:forms/show-v-forms])})
        (checkbox props {:name :downshift-ordinals :text (tr [:forms/downshift-ordinals])})
        (checkbox props {:name :enable-capitalization :text (tr [:forms/enable-capitalization])})
        (dropdown props {:name :detailed-accented-chars :label (tr [:forms/accented-chars])
                               :options [{:swiss (tr [:accented-chars/swiss])} {:basic (tr [:accented-chars/basic])}]})
        (dropdown props {:name :footnote-placement :label (tr [:forms/footnote-placement])
                               :options [{:standard (tr [:footnote-placement/standard])} {:end-vol (tr [:footnote-placement/end-vol])}
                                         {:level1 (tr [:footnote-placement/level1])} {:level2 (tr [:footnote-placement/level2])}
                                         {:level3 (tr [:footnote-placement/level3])} {:level4 (tr [:footnote-placement/level4])}]})
        (submit-button props {:name :submit :label (tr [:preview])})]))])

(defn large-print-library [{id :id}]
  [fork/form {:initial-values {}
              :path [:form :large-print-library]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit-handler :large-print id %])
              :keywordize-keys true
              }
   (fn [{:keys [path
                form-id
                submitting?
		on-submit-server-message
                handle-submit] :as props}]
     (if on-submit-server-message
       [] ; [forms/error-notification on-submit-server-message path]
       [:form {:id form-id :on-submit handle-submit}
        (submit-button props {:name :submit :label (tr [:preview])})]))])

(defn large-print-sale [{id :id}]
  [fork/form {:initial-values {:font-size 17}
              :path [:form :large-print-sale]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit-handler :large-print id %])
              :keywordize-keys true
              }
   (fn [{:keys [path
                form-id
                submitting?
		on-submit-server-message
                handle-submit] :as props}]
     (if on-submit-server-message
       [] ; [forms/error-notification on-submit-server-message path]
       [:form {:id form-id :on-submit handle-submit}
        (dropdown props {:name :font-size :label (tr [:forms/font-size]) :options [{17 "17pt"} {20 "20pt"} {25 "25pt"}]})
        (submit-button props {:name :submit :label (tr [:preview])})]))])

(defn large-print-configurable [{id :id}]
  [fork/form {:initial-values {:font-size 17
                               :font :tiresias
                               :page-style :plain
                               :alignment :left
                               :stock-size :a4paper
                               :line-spacing :onehalfspacing
                               :replace-em-with-quote true
                               :end-notes :none
                               :image-visibility :ignore}
              :path [:form :large-print-configurable]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::submit-handler :large-print id %])
              :keywordize-keys true
              }
   (fn [{:keys [path
                form-id
                submitting?
		on-submit-server-message
                handle-submit] :as props}]
     (if on-submit-server-message
       [] ; [forms/error-notification on-submit-server-message path]
       [:form {:id form-id :on-submit handle-submit}
        (dropdown props {:name :font-size :label (tr [:forms/font-size])
                               :options [{17 "17pt"} {20 "20pt"} {25 "25pt"}]})
        (dropdown props {:name :font :label (tr [:forms/font])
                               :options [{:tiresias "Tiresias LPfont"} {:roman "Latin Modern Roman"} {:sans "Latin Modern Sans"} {:mono "Latin Modern Mono"}]})
        (dropdown props {:name :page-style :label (tr [:forms/page-style])
                               :options [{:plain (tr [:page-style/plain])} {:with-page-nums (tr [:page-style/with-page-nums])}
                                         {:spacious (tr [:page-style/spacious])} {:scientific (tr [:page-style/scientific])}]})
        (dropdown props {:name :alignment :label (tr [:forms/alignment])
                               :options [{:left (tr [:alignment/left])} {:justified (tr [:alignment/justified])}]})
        (dropdown props {:name :stock-size :label (tr [:forms/stock-size])
                               :options [{:a3paper (tr [:stock-size/a3paper])} {:a4paper (tr [:stock-size/a4paper])}]})
        (dropdown props {:name :line-spacing :label (tr [:forms/line-spacing])
                               :options [{:singlespacing (tr [:line-spacing/singlespacing])} {:onehalfspacing (tr [:line-spacing/onehalfspacing])}
                                         {:doublespacing (tr [:line-spacing/doublespacing])}]})
        (checkbox props {:name :replace-em-with-quote :text (tr [:forms/replace-em-with-quote])})
        (dropdown props {:name :end-notes :label (tr [:forms/end-notes])
                               :options [{:none (tr [:end-notes/none])} {:document (tr [:end-notes/document])} {:chapter (tr [:end-notes/chapter])}]})
        (dropdown props {:name :image-visibility :label (tr [:forms/image-visibility])
                               :options [{:show (tr [:image-visibility/show])} {:ignore (tr [:image-visibility/ignore])}]})
        (submit-button props {:name :submit :label (tr [:preview])})]))])

(defn open-document [{id :id}]
  [fork/form {:initial-values {:math :both
                               :phonetics true
                               :image-inclusion :linked
                               :line-numbers true
                               :page-numbers true
                               :floating-page-numbers true
                               :answer-markup "_.."}
              :path [:form :open-document]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::preview-open-document id %])
              :keywordize-keys true
              }
   (fn [{:keys [path
                form-id
                submitting?
		on-submit-server-message
                handle-submit] :as props}]
     (if on-submit-server-message
       [] ; [forms/error-notification on-submit-server-message path]
       [:form {:id form-id :on-submit handle-submit}
        (dropdown props {:name :math :label (tr [:forms/math])
                               :options [{:asciimath (tr [:math/asciimath])} {:mathml (tr [:math/mathml])} {:both (tr [:math/both])}]})
        (checkbox props {:name :phonetics :text (tr [:forms/phonetics])})
        (dropdown props {:name :image-inclusion :label (tr [:forms/image-inclusion])
                               :options [{:drop (tr [:image-inclusion/drop])} {:link (tr [:image-inclusion/link])} {:embed (tr [:image-inclusion/embed])}]})
        (checkbox props {:name :line-numbers :text (tr [:forms/line-numbers])})
        (checkbox props {:name :page-numbers :text (tr [:forms/page-numbers])})
        (checkbox props {:name :floating-page-numbers :text (tr [:forms/floating-page-numbers])})
        (input props {:name :answer-markup :label (tr [:forms/answer-markup]) :type "text"})
        (submit-button props {:name :submit :label (tr [:preview])})]))])
