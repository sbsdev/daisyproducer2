(ns daisyproducer2.documents.preview.forms
  (:require [daisyproducer2.i18n :refer [tr]]
            [fork.re-frame :as fork]
            [re-frame.core :as rf]))

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
                               :depth-of-toc 0 :footer-up-to-level 0
                               :include-sbsform-macros true :show-page-numbers true
                               :show-vforms true :downshift-ordinals true
                               :enable-capitalization false
                               :accented-chars :swiss :footnote-placement :standard}
              :path [:form :braille]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::preview-braille id %])
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
        (input props {:name :cells-per-line :label (tr [:forms/cells-per-line]) :type "text"})
        (input props {:name :lines-per-page :label (tr [:forms/lines-per-page]) :type "text"})
        (dropdown props {:name :contraction :label (tr [:forms/contraction])
                               :options [{0 (tr [:grade/g0])} {1 (tr [:grade/g1])} {2 (tr [:grade/g2])}]})
        (checkbox props {:name :hyphenation :text (tr [:forms/hyphenation])})
        (dropdown props {:name :depth-of-toc :label (tr [:forms/depth-of-toc])
                               :options [{0 0} {1 1} {2 2} {3 3} {4 4} {5 5} {6 6}]})
        (dropdown props {:name :footer-up-to-level :label (tr [:forms/footer-up-to-level])
                               :options [{0 0} {1 1} {2 2} {3 3} {4 4} {5 5} {6 6}]})
        (checkbox props {:name :include-sbsform-macros :text (tr [:forms/include-sbsform-macros])})
        (checkbox props {:name :show-page-numbers :text (tr [:forms/show-page-numbers])})
        (checkbox props {:name :show-vforms :text (tr [:forms/show-vforms])})
        (checkbox props {:name :downshift-ordinals :text (tr [:forms/downshift-ordinals])})
        (checkbox props {:name :enable-capitalization :text (tr [:forms/enable-capitalization])})
        (dropdown props {:name :accented-chars :label (tr [:forms/accented-chars])
                               :options [{:swiss (tr [:accented-chars/swiss])} {:basic (tr [:accented-chars/basic])}]})
        (dropdown props {:name :footnote-placement :label (tr [:forms/footnote-placement])
                               :options [{:standard (tr [:footnote-placement/standard])} {:end-vol (tr [:footnote-placement/end-vol])}
                                         {:level1 (tr [:footnote-placement/level1])} {:level2 (tr [:footnote-placement/level2])}
                                         {:level3 (tr [:footnote-placement/level3])} {:level4 (tr [:footnote-placement/level4])}]})
        (submit-button props {:name :submit :label (tr [:preview])})]))])

(defn large-print-sale [{id :id}]
  [fork/form {:initial-values {:font-size 17}
              :path [:form :large-print-sale]
              :prevent-default? true
              :clean-on-unmount? true
              :on-submit #(rf/dispatch [::preview-large-print-sale id %])
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
              :on-submit #(rf/dispatch [::preview-large-print-configurable id %])
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