(ns daisyproducer2.documents.preview
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [re-frame.core :as rf]))

(defn- tooltip-button [{:keys [tooltip icon href label] :as opts}]
  ;; if we have an href we need an anchor element. Otherwise use a button
  (let [element (if href :a.button.has-tooltip-arrow :button.button.has-tooltip-arrow)]
    [element
     (merge
      {:data-tooltip (tr [tooltip])
       :aria-label (tr [tooltip])}
      (dissoc opts :tooltip :icon))
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]
     (when label [:span (tr [label])])]))

(defn- label-button [{:keys [label icon href] :as opts}]
  ;; if we have an href we need an anchor element. Otherwise use a button
  (let [element (if href :a.button :button.button)]
    [element
     (dissoc opts :label :icon)
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]
     (when label [:span (tr [label])])]))

(defn preview-links [document]
  [:div.block
   [:table.table
    [:tbody
     [:tr
      [:th (tr [:braille])]
      [:td [:div.field.is-grouped
            [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]]]]
     [:tr
      [:th (tr [:epub3])]
      [:td [:div.field.is-grouped
            [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]
            [:p.control [label-button {:label :open-in-online-player :icon "mi-open-in-new" }]]]]]
     [:tr
      [:th (tr [:large-print])]
      [:td [:div.field.is-grouped
            [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]]]]]]])


