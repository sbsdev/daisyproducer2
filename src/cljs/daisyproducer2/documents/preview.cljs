(ns daisyproducer2.documents.preview
  (:require [daisyproducer2.i18n :refer [tr]]))

(defn- braille-buttons [id]
  [:div.field
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/braille")}
     [:span (tr [:preview])]]]])

(defn large-print-buttons [id]
  [:div.field.is-grouped
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/large-print-library")}
     [:span (tr [:large-print-library])]]]
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/large-print-sale")}
     [:span (tr [:large-print-sale])]]]
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/large-print-configurable")}
     [:span (tr [:large-print-configurable])]]]])

(defn epub-buttons [id]
  [:div.field.is-grouped
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/epub")}
     [:span (tr [:preview])]]]
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/epub-in-player")}
     [:span (tr [:online-player])]
     [:span.icon {:aria-hidden true} [:i.mi.mi-open-in-new]]]]])

(defn- open-document-buttons [id]
  [:div.field
   [:p.control
    [:a.button
     {:href (str "#/documents/" id "/preview/open-document")}
     [:span (tr [:preview])]]]])

(defn preview-links [{id :id}]
  [:div.block
     [:table.table.is-fullwidth
      [:thead
       [:tr
        [:th {:width "100%"} (tr [:format])]
        [:th (tr [:action])]]]
      [:tbody
       [:tr [:th (tr [:braille])] [:td [braille-buttons id]]]
       [:tr [:th (tr [:large-print])] [:td [large-print-buttons id]]]
       [:tr [:th (tr [:epub3])] [:td [epub-buttons id]]]
       [:tr [:th (tr [:open-document])] [:td [open-document-buttons id]]]]]])


