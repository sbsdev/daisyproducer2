(ns daisyproducer2.core
  (:require
    [day8.re-frame.http-fx]
    [reagent.dom :as rdom]
    [reagent.core :as r]
    [re-frame.core :as rf]
    [goog.events :as events]
    [goog.history.EventType :as HistoryEventType]
    [markdown.core :refer [md->html]]
    [daisyproducer2.ajax :as ajax]
    [daisyproducer2.events]
    [daisyproducer2.auth :as auth]
    [daisyproducer2.documents :as documents]
    [daisyproducer2.documents.document :as document]
    [daisyproducer2.documents.image :as image]
    [daisyproducer2.documents.version :as version]
    [daisyproducer2.documents.state :as state]
    [daisyproducer2.hyphenations :as hyphenations]
    [daisyproducer2.words :as words]
    [daisyproducer2.words.global :as global]
    [daisyproducer2.words.confirm :as confirm]
    [daisyproducer2.i18n :refer [tr]]
    [reitit.core :as reitit]
    [reitit.frontend.easy :as rfe])
  (:import goog.History))

(defn nav-link [uri title page]
  [:a.navbar-item
   {:href   uri
    :class (when (= page @(rf/subscribe [:common/page-id])) :is-active)}
   title])

(defn navbar []
  (r/with-let [expanded? (r/atom false)]
    (let [is-admin? @(rf/subscribe [::auth/is-admin?])]
      [:nav.navbar.is-info>div.container
       [:div.navbar-brand
        [:span.navbar-item {:style {:font-weight :bold}} "daisyproducer2"]
        [:span.navbar-burger.burger
         {:data-target :nav-menu
          :on-click #(swap! expanded? not)
          :class (when @expanded? :is-active)}
         [:span][:span][:span]]]
       [:div#nav-menu.navbar-menu
        {:class (when @expanded? :is-active)}
        [:div.navbar-start
         [nav-link "#/" (tr [:documents]) :documents]
         [nav-link "#/hyphenations" (tr [:hyphenations]) :hyphenations]
         (when is-admin? [nav-link "#/confirm" (tr [:confirm]) :confirm])
         [nav-link "#/words" (tr [:words]) :words]]
        [:div.navbar-end
         [:div.navbar-item
          (auth/user-buttons)]]]])))

(defn page []
  (if-let [page @(rf/subscribe [:common/page])]
    [:div
     [navbar]
     [page]]))

(defn navigate! [match _]
  (rf/dispatch [:common/navigate match]))

(def router
  (reitit/router
    [["/" {:name        :documents
           :view        #'documents/page
           :controllers [{:start (fn [_] (rf/dispatch [::documents/init-documents]))}]}]
     ["/login" {:name :login
                :view #'auth/login-page}]
     ["/documents/:id" {:controllers
                        [{:parameters {:path [:id]}
                          :start (fn [params] (rf/dispatch [::document/init-current (-> params :path :id)]))
                          :stop (fn [_] (rf/dispatch [::document/clear-current]))}]}
      ["" {:name :document :view #'document/page
            :controllers [{:parameters {:path [:id]}}]}]
      ["/unknown" {:name :document-unknown :view #'document/unknown
                   :controllers [{:parameters {:path [:id]}}]}]
      ["/local" {:name :document-local :view #'document/local
                 :controllers [{:parameters {:path [:id]}}]}]
      ["/preview" {:name :document-preview :view #'document/preview
                   :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/braille" {:name :document-preview-braille :view #'document/preview-braille
                           :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/large-print-library" {:name :document-preview-large-print-library :view #'document/preview-large-print-library
                                    :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/large-print-sale" {:name :document-preview-large-print-sale :view #'document/preview-large-print-sale
                                    :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/large-print-configurable" {:name :document-preview-large-print-configurable :view #'document/preview-large-print-configurable
                                            :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/epub" {:name :document-preview-epub :view #'document/preview-epub
                                 :controllers [{:parameters {:path [:id]}}]}]
      ["/preview/open-document" {:name :document-preview-open-document :view #'document/preview-open-document
                                 :controllers [{:parameters {:path [:id]}}]}]
      ["/markup" {:name :document-markup :view #'document/markup
                   :controllers [{:parameters {:path [:id]}}]}]
      ["/versions" {:name :document-versions :view #'document/versions
                    :controllers [{:parameters {:path [:id]}}]}]
      ["/versions/upload" {:name :document-versions-upload :view #'document/versions-upload
                           :controllers [{:parameters {:path [:id]}}]}]
      ["/images" {:name :document-images :view #'document/images
                  :controllers [{:parameters {:path [:id]}}]}]
      ["/images/upload" {:name :document-images-upload :view #'document/images-upload
                         :controllers [{:parameters {:path [:id]}}]}]]
     ["/hyphenations" {:name :hyphenations
                       :view #'hyphenations/add-page}]
     ["/hyphenations/edit" {:name :hyphenations-edit
                            :view #'hyphenations/edit-page}]
     ["/confirm" {:name :confirm
                  :view #'confirm/words-page
                  :controllers [{:start (fn [_] (rf/dispatch [::confirm/fetch-words]))}]}]
     ["/words" {:name :words
                :view #'global/words-page}]]))

(defn start-router! []
  (rfe/start!
    router
    navigate!
    {}))

;; -------------------------
;; Initialize app
(defn ^:dev/after-load mount-components []
  (rf/clear-subscription-cache!)
  (rdom/render [#'page] (.getElementById js/document "app")))

(defn init! []
  (start-router!)
  (ajax/load-interceptors!)
  (mount-components))
