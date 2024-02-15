(ns daisyproducer2.documents.preview
  (:require [daisyproducer2.i18n :refer [tr]]))

(defn preview-links [{id :id}]
  [:div.block
     [:table.table
      [:thead
       [:tr
        [:th {:width "100%"} (tr [:format])]
        [:th (tr [:action])]]]
      [:tbody
       [:tr
        [:th (tr [:epub3])]
        [:td [:div.field.is-grouped
              [:p.control
               [:a.button
                {:href (str "/api/documents/" id "/preview/epub")
                 ;;:download "download"
                 :target "_blank"}
                [:span (tr [:download])]
                [:span.icon {:aria-hidden true}
                 [:i.mi.mi-download]]]]
              [:p.control
               [:a.button
                {:href (str "/api/documents/" id "/preview/epub-in-player")
                 :target "_blank"}
                [:span (tr [:online-player])]
                [:span.icon {:aria-hidden true}
                 [:i.mi.mi-open-in-new]]]]]]]]]])


