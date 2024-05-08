(ns daisyproducer2.documents.document
  (:require [ajax.core :as ajax]
            [clojure.string :as string]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.documents.image :as image]
            [daisyproducer2.documents.version :as version]
            [daisyproducer2.documents.preview :as preview]
            [daisyproducer2.documents.preview.forms :as forms]
            [daisyproducer2.documents.markup :as markup]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::current
 (fn [db _]
   (-> db :current-document)))

(rf/reg-event-db
  ::set-current
  (fn [db [_ document]]
    (assoc db :current-document document)))

(rf/reg-event-db
  ::clear-current
  (fn [db [_]]
    (dissoc db :current-document)))

(rf/reg-event-fx
  ::fetch-current
  (fn [_ [_ id]]
    {:http-xhrio {:method          :get
                  :uri             (str "/api/documents/" id)
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success      [::set-current]}}))

(rf/reg-event-fx
  ::init-current
  (fn [{:keys [db]} [_ id]]
    {:dispatch-n [[::fetch-current id]
                  [::unknown/fetch-words-total id]]}))


(defn tab-link-with-total [uri title page on-click]
  (let [total @(rf/subscribe [::unknown/words-total])
        tag [:span.tag.is-rounded total]]
    (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
      [:li.is-active [:a title tag]]
      [:li [:a {:href uri :on-click on-click} title tag]])))

(defn tab-link [uri title page on-click]
  (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
    [:li.is-active [:a title]]
    [:li [:a {:href uri :on-click on-click} title]]))

(defn tabs [{:keys [id]}]
  [:div.block
   [:div.tabs.is-boxed
    [:ul
     [tab-link (str "#/documents/" id) (tr [:details]) :document]
     [tab-link-with-total (str "#/documents/" id "/unknown") (tr [:unknown-words]) :document-unknown
      (fn [_] (rf/dispatch [::unknown/fetch-words id]) (rf/dispatch [::unknown/fetch-words-total id]))]
     [tab-link (str "#/documents/" id "/local") (tr [:local-words]) :document-local (fn [_] (rf/dispatch [::local/fetch-words id]))]
     [tab-link (str "#/documents/" id "/versions") (tr [:versions]) :document-versions (fn [_] (rf/dispatch [::version/fetch-versions id]))]
     [tab-link (str "#/documents/" id "/images") (tr [:images]) :document-images (fn [_] (rf/dispatch [::image/fetch-images id]))]
     [tab-link (str "#/documents/" id "/markup") (tr [:markup]) :document-markup (fn [_] (rf/dispatch [::markup/fetch-latest-version id]))]
     [tab-link (str "#/documents/" id "/preview") (tr [:preview]) :document-preview]
     ]]])

(defn summary [{:keys [title author source-publisher state] :as document}]
  [:div.columns
   [:div.column
    [:div.block
     [:table.table
      [:tbody
       [:tr [:th {:width 200} (tr [:title])] [:td title]]
       [:tr [:th (tr [:author])] [:td author]]
       [:tr [:th (tr [:source-publisher])] [:td source-publisher]]
       [:tr [:th (tr [:state])] [:td [state/state state]]]]]]]
   [:div.column.is-narrow
    [state/button document]]])

(defn details [document]
  [:div.block
   [:table.table.is-striped
    [:tbody
     (for [k [:date :modified-at :spelling :language]
           :let [v (case k
                     :spelling (words/spelling-brief-string (get document k))
                     (get document k))]
           :when (not (string/blank? v))]
       ^{:key k}
       [:tr [:th (tr [k])] [:td v]])]]])

(defn page []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [details document]]))

(defn unknown []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [grade/selector ::unknown/fetch-words]
     [unknown/unknown-words]]))

(defn local []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [grade/selector ::local/fetch-words]
     [local/local-words]]))

(defn preview []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [preview/preview-links document]]))

(defn preview-braille []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/braille document]]))

(defn preview-large-print-library []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/large-print-library document]]))

(defn preview-large-print-sale []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/large-print-sale document]]))

(defn preview-large-print-configurable []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/large-print-configurable document]]))

(defn preview-epub []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/epub document]]))

(defn preview-epub-in-player []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/epub-in-player document]]))

(defn preview-open-document []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [forms/open-document document]]))

(defn markup []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [markup/markup document]]))

(defn versions []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [version/versions document]]))

(defn versions-upload []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [version/upload document]]))

(defn images []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [image/images document]]))

(defn images-upload []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [image/upload document]]))
