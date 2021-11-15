(ns daisyproducer2.words.input-fields
  (:require [clojure.string :as str]
            [daisyproducer2.i18n :refer [tr]]
            [re-frame.core :as rf]
            [reagent.core :as reagent]))

(rf/reg-sub
 ::word-field
 (fn [db [_ page id field-id]]
   (get-in db [:words page id field-id])))

(rf/reg-event-db
 ::set-word-field
 (fn [db [_ page id field-id value]]
   (assoc-in db [:words page id field-id] value)))

(defn local-field [page id]
  (let [value @(rf/subscribe [::word-field page id :islocal])
        html-id (str "local-field-" id)]
    [:<>
     [:label.is-sr-only {:for html-id} (tr [:local])]
     [:input {:type "checkbox"
              :id html-id
              :aria-checked value
              :checked value
              :on-change #(rf/dispatch [::set-word-field page id :islocal (not value)])}]]))

;; An input-field component keeps its value as component local state.
;; It only updates the app state when saving by pressing RET or on
;; blur. Much of this is inspired by the reframe todomvc example at
;; https://github.com/day8/re-frame/blob/master/examples/todomvc/src/todomvc/views.cljs#L7
(defn input-field [page id field-id validator]
  (let [initial-value @(rf/subscribe [::word-field page id field-id])
        value (reagent/atom initial-value)
        stop #(reset! value initial-value)
        save #(let [v (-> @value str str/trim)]
                (rf/dispatch [::set-word-field page id field-id v]))]
    (fn []
      (let [valid? (validator @value)
            changed? (not= initial-value @value)
            klass (list (cond (not valid?) "is-danger"
                              changed? "is-warning")
                        ;; braille fields should be in mono space
                        (when (#{:contracted :uncontracted} field-id) "braille"))]
        [:div.field
         [:input.input {:type "text"
                        :aria-label (tr [field-id])
                        :class klass
                        :value @value
                        :on-blur save
                        :on-change #(reset! value (-> % .-target .-value))
                        :on-key-down #(case (.-which %)
                                        13 (save) ; RET
                                        27 (stop) ; ESC
                                        nil)}]
         (when-not valid?
           [:p.help.is-danger (tr [:input-not-valid])])]))))

(defn disabled-field [value]
  [:div.field
   [:input.input {:type "text" :value value :disabled "disabled"}]])
