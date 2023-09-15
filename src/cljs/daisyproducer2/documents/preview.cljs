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
     (when label [:span (tr [label])])
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]]))

(defn- label-button [{:keys [label icon href] :as opts}]
  ;; if we have an href we need an anchor element. Otherwise use a button
  (let [element (if href :a.button :button.button)]
    [element
     (dissoc opts :label :icon)
     (when label [:span (tr [label])])
     [:span.icon {:aria-hidden true}
      [:i.mi {:class icon}]]]))

(defn preview-links [document]
  [:div.block
    [:table.table
     [:thead
      [:tr
       [:th {:width "100%"} (tr [:format])]
       [:th (tr [:action])]]]
     [:tbody
      #_[:tr
       [:th (tr [:braille])]
       [:td [:div.field.is-grouped
             [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]]]]
      [:tr
       [:th (tr [:epub3])]
       [:td [:div.field.is-grouped
             [:p.control [label-button {:label :download :icon "mi-download"}]]
             [:p.control [label-button {:label :open-in-online-player :icon "mi-open-in-new" }]]]]]
      #_[:tr
       [:th (tr [:large-print])]
       [:td [:div.field.is-grouped
             [:p.control [label-button {:label "Library" :icon "mi-download"}]]
             [:p.control [label-button {:label "Sale" :icon "mi-download"}]]
             [:p.control [label-button {:label "Configurable" :icon "mi-download"}]]]]]
      #_[:tr
       [:th (tr [:open-document "Open Document"])]
       [:td [:div.field.is-grouped
             [:p.control [tooltip-button {:tooltip :download :icon "mi-download"}]]]]]]]])


