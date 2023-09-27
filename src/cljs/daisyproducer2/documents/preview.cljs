(ns daisyproducer2.documents.preview
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

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


