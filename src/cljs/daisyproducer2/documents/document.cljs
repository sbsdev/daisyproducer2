(ns daisyproducer2.documents.document
  (:require [ajax.core :as ajax]
            [clojure.string :as str]
            [daisyproducer2.documents.state :as state]
            [daisyproducer2.documents.details :as details]
            [daisyproducer2.documents.image :as image]
            [daisyproducer2.documents.product :as product]
            [daisyproducer2.documents.version :as version]
            [daisyproducer2.documents.preview :as preview]
            [daisyproducer2.documents.preview.forms :as forms]
            [daisyproducer2.documents.markup :as markup]
            [daisyproducer2.i18n :refer [tr]]
            [daisyproducer2.words :as words]
            [daisyproducer2.words.grade :as grade]
            [daisyproducer2.words.local :as local]
            [daisyproducer2.words.unknown :as unknown]
            [daisyproducer2.words.notifications :as notifications]
            [re-frame.core :as rf]))

(rf/reg-sub
 ::current
 (fn [db _]
   (-> db :current-document)))

(rf/reg-sub
 ::current-is-german
 :<- [::current]
 (fn [current] (#{"de" "de-1901"} (-> current :language))))

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
                  [::product/fetch-products id]
                  [::unknown/fetch-words-total id]
                  [::local/fetch-words-total id]]}))


(defn tab-link-with-total [uri title page subscription on-click]
  (let [total @(rf/subscribe subscription)
        tag [:span.tag.is-rounded total]]
    (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
      [:li.is-active [:a title tag]]
      [:li [:a {:href uri :on-click on-click} title tag]])))

(defn tab-link [uri title page on-click]
  (if-let [is-active (= page @(rf/subscribe [:common/page-id]))]
    [:li.is-active [:a title]]
    [:li [:a {:href uri :on-click on-click} title]]))

(defn tabs [{:keys [id]}]
  (let [german? @(rf/subscribe [::current-is-german])]
    [:div.block
     [:div.tabs.is-boxed
      [:ul
       [tab-link (str "#/documents/" id) (tr [:details]) :document]
       ;; only show the unknown and local words for German books
       (when german?
         [tab-link-with-total (str "#/documents/" id "/unknown") (tr [:unknown-words]) :document-unknown [::unknown/words-total]
          (fn [_] (rf/dispatch [::unknown/fetch-words id]) (rf/dispatch [::unknown/fetch-words-total id]))])
       (when german?
         [tab-link-with-total (str "#/documents/" id "/local") (tr [:local-words]) :document-local [::local/words-total]
          (fn [_] (rf/dispatch [::local/fetch-words id]) (rf/dispatch [::local/fetch-words-total id]))])
       [tab-link (str "#/documents/" id "/versions") (tr [:versions]) :document-versions (fn [_] (rf/dispatch [::version/fetch-versions id]))]
       [tab-link (str "#/documents/" id "/images") (tr [:images]) :document-images (fn [_] (rf/dispatch [::image/fetch-images id]))]
       [tab-link (str "#/documents/" id "/markup") (tr [:markup]) :document-markup (fn [_] (rf/dispatch [::markup/fetch-latest-version id]))]
       [tab-link (str "#/documents/" id "/preview") (tr [:preview]) :document-preview]
       ]]]))

(defn summary [{:keys [title author source-publisher state] :as document}]
  [:div.block
     [:table.table.is-striped.is-fullwidth
      [:tbody
       [:tr [:th {:width 200} (tr [:title])] [:td title]]
       [:tr [:th (tr [:author])] [:td author]]
       [:tr [:th (tr [:source-publisher])] [:td source-publisher]]
       [:tr [:th (tr [:state])] [:td [state/state state]]]]]])

(defn details [document]
  [:div.block
   [:table.table.is-striped.is-fullwidth
    [:tbody
     (for [k [:date :modified-at :spelling :language]
           :let [v (case k
                     :spelling (words/spelling-brief-string (get document k))
                     (get document k))]
           :when (not (str/blank? v))]
       ^{:key k}
       [:tr [:th (tr [k])] [:td v]])]]])

(defn buttons [document]
  (if-let [errors? @(rf/subscribe [::notifications/errors?])]
    [notifications/error-notification]
    [:div.block
     [:div.field.is-grouped
      [:p.control
       [state/button document]]
      [:p.control
       [details/synchronize-button document]]]]))

(defn page []
  (let [document @(rf/subscribe [::current])]
    [:section.section>div.container>div.content
     [summary document]
     [tabs document]
     [details document]
     [product/products document]
     [buttons document]]))

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
