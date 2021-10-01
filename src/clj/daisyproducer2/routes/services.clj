(ns daisyproducer2.routes.services
  (:require
    [daisyproducer2.db.core :as db]
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [spec-tools.data-spec :as spec]
    [clojure.spec.alpha :as s]
    [daisyproducer2.middleware :refer [wrap-restricted wrap-authorized]]
    [daisyproducer2.middleware.formats :as formats]
    [daisyproducer2.middleware.exception :as exception]
    [ring.util.http-response :refer :all]
    [clojure.java.io :as io]
    [clojure.string :refer [blank?]]
    [daisyproducer2.documents :as docs]
    [daisyproducer2.auth :as auth]
    [daisyproducer2.hyphenate :as hyphenate]
    [daisyproducer2.validation :as validation]
    [daisyproducer2.documents.versions :as versions]
    [daisyproducer2.documents.images :as images]
    [daisyproducer2.words.unknown :as unknown]
    [daisyproducer2.words.local :as local]
    [daisyproducer2.words.confirm :as confirm]
    [daisyproducer2.words.global :as global]))

(s/def ::grade (s/and int? #(<= 0 % 2)))
(s/def ::spelling (s/and int? #{0 1}))
(s/def ::braille (s/and string? validation/braille-valid?))
(s/def ::hyphenation (s/and string? validation/hyphenation-valid?))

(def default-limit 100)

(defn service-routes []
  ["/api"
   {:coercion spec-coercion/coercion
    :muuntaja formats/instance
    :swagger {:id ::api
              :securityDefinitions {:apiAuth
                                    {:type "apiKey"
                                     :name "Authorization"
                                     :in "header"}}}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 exception/exception-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc true
        :swagger {:info {:title "Daisyproducer API Reference"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/login"
    {:post
     {:summary    "Handle user login"
      :tags       ["Authentication"]
      :parameters {:body {:username string?
                          :password string?}}
      :handler    (fn [{{{:keys [username password]} :body} :parameters}]
                    (if-let [credentials (auth/login username password)]
                      (ok credentials) ; return token and user info
                      (bad-request
                       {:message "Cannot authenticate user with given password"})))}}]

   ["/documents"
    {:swagger {:tags ["Documents"]}}

    [""
     {:get {:summary "Get all documents"
            :description "Get all documents. Optionally limit the result set using a `search` term, a `limit` and an `offset`."
            :parameters {:query {(spec/opt :search) string?
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [limit offset search]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (ok (if (blank? search)
                             (db/get-documents {:limit limit :offset offset})
                             (db/find-documents {:limit limit :offset offset :search (db/search-to-sql search)}))))}}]

    ["/:id"
     {:get {:summary "Get a document by ID"
            :parameters {:path {:id int?}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [doc (db/get-document {:id id})]
                         (ok doc)
                         (not-found)))}}]]

   ["/words"
    {:swagger {:tags ["Global Words"]}}

    [""
     {:get {:summary "Get global words. Optionally filter the results by using `untranslated`, `limit` and `offset`."
            :parameters {:query {(spec/opt :untranslated) string?
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [untranslated limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (ok (global/get-words {:untranslated untranslated :limit limit :offset offset})))}

      :put {:summary "Update or create a global word"
            :middleware [wrap-restricted wrap-authorized]
            :swagger {:security [{:apiAuth []}]}
            :parameters {:body {:untranslated string?
                                :type int?
                                :uncontracted ::braille
                                :contracted ::braille
                                :homograph-disambiguation string?}}
            :handler (fn [{{word :body} :parameters}]
                       (global/put-word word)
                       (no-content))}

      :delete {:summary "Delete a global word"
               :middleware [wrap-restricted wrap-authorized]
               :swagger {:security [{:apiAuth []}]}
               :parameters {:body {:untranslated string?
                                   :type int?
                                   :uncontracted ::braille
                                   :contracted ::braille
                                   :homograph-disambiguation string?}}
               :handler (fn [{{word :body} :parameters}]
                          (let [deleted (global/delete-word word)]
                            (if (>= deleted 1)
                              (no-content)
                              (not-found))))}}]

    ["/:untranslated"
     {:get {:summary "Get global words by untranslated"
            :parameters {:path {:untranslated string?}
                         :query {(spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [untranslated]} :path
                            {:keys [limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (if-let [words (not-empty (global/get-words {:untranslated untranslated :limit limit :offset offset}))]
                         (ok words)
                         (not-found)))}}]]

   ["/documents/:id"

    ["/words"
     {:swagger {:tags ["Local Words"]}
      :get {:summary "Get all local words for a given document. Optionally filter the results by using a search string, a limit and an offset."
            :parameters {:path {:id int?}
                         :query {:grade ::grade
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?
                                 (spec/opt :search) string?}}
            :handler (fn [{{{:keys [id]} :path
                            {:keys [grade search limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (if-let [words (local/get-words id grade search limit offset)]
                         (ok words)
                         (not-found)))}

      :put {:summary "Update or create a local word for a given document"
            :middleware [wrap-restricted]
            :swagger {:security [{:apiAuth []}]}
            :parameters {:body {:untranslated string? :type int?
                                (spec/opt :uncontracted) ::braille
                                (spec/opt :contracted) ::braille
                                :homograph-disambiguation string?
                                :document-id int? :islocal boolean?
                                :hyphenated (spec/maybe ::hyphenation)
                                :spelling ::spelling}}
            :handler (fn [{{word :body} :parameters}]
                       (local/put-word word)
                       (no-content))}

      :delete {:summary "Delete a local word for a given document"
               :middleware [wrap-restricted]
               :swagger {:security [{:apiAuth []}]}
               :parameters {:body {:untranslated string? :type int?
                                   (spec/opt :uncontracted) ::braille
                                   (spec/opt :contracted) ::braille
                                   :homograph-disambiguation string?
                                   :document-id int?
                                   :hyphenated (spec/maybe ::hyphenation)
                                   :spelling ::spelling}}
               :handler (fn [{{word :body} :parameters}]
                          (let [deleted (local/delete-word word)]
                            (if (>= deleted 1)
                              (no-content) ; we found something and deleted it
                              (not-found))))}}] ; couldn't find and delete the requested resource

    ["/unknown-words"
     {:swagger {:tags ["Unknown Words"]}
      :get {:summary "Get all unknown words for a given document"
            :parameters {:path {:id int?}
                         :query {:grade ::grade
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [id]} :path
                            {:keys [grade limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (let [version (docs/get-latest-version id)
                             unknown (unknown/get-words version id grade limit offset)]
                         (ok unknown)))}}]

    ["/versions"
     {:swagger {:tags ["Versions"]}}

     [""
      {:get {:summary "Get all versions of a given document"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [versions (not-empty (versions/get-versions id))]
                          (ok versions)
                          (not-found)))}
       :post {:summary "Create a new version for a given document"
              :middleware [wrap-restricted]
              :swagger {:security [{:apiAuth []}]}
              :parameters {:path {:id int?}
                           :multipart {:file multipart/temp-file-part
                                       :comment string?}}
              :handler (fn [{{{:keys [id]} :path {{tempfile :tempfile} :file comment :comment} :multipart} :parameters
                             {{uid :uid} :user} :identity}]
                         (let [new-key (versions/insert-version id tempfile comment uid)
                               new-url (format "/documents/%s/versions/%s" id new-key)]
                           (created new-url)))}}]
     ;; FIXME: implement latest as a filter on get all versions GET /documents/123/versions?latest=true
     #_["/latest"
      {:get {:summary "Get the latest version of a given document"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [doc (db/get-latest-version {:document_id id})]
                          (ok doc)
                          (not-found)))}}]
     ["/:version-id"
      {:get {:summary "Get a version"
             :parameters {:path {:id int? :version-id int?}}
             :handler (fn [{{{:keys [id version-id]} :path} :parameters}]
                        (if-let [version (versions/get-version id version-id)]
                          (ok version)
                          (not-found)))}
       :delete {:summary "Delete a version"
                :middleware [wrap-restricted]
                :swagger {:security [{:apiAuth []}]}
                :parameters {:path {:id int? :version-id int?}}
                :handler (fn [{{{:keys [id version-id]} :path} :parameters}]
                           (let [deleted (versions/delete-version id version-id)]
                             (case deleted
                               true (no-content)
                               false (internal-server-error) ; we found something but could not delete the content
                               nil (not-found))))}}]]

    ["/images"
     {:swagger {:tags ["Images"]}}

     [""
      {:get {:summary "Get all images of a given document"
             :parameters {:path {:id int?}
                          :query {(spec/opt :limit) int?
                                  (spec/opt :offset) int?}}
             :handler (fn [{{{:keys [id]} :path
                             {:keys [limit offset]
                              :or {limit default-limit offset 0}} :query} :parameters}]
                        (ok (images/get-images id limit offset)))}
       :post {:summary "Add a new image to a given document"
              :middleware [wrap-restricted]
              :swagger {:security [{:apiAuth []}]}
              :parameters {:path {:id int?} :multipart {:file multipart/temp-file-part}}
              :handler (fn [{{{:keys [id]} :path {{:keys [filename tempfile]} :file} :multipart} :parameters
                             {{uid :uid} :user} :identity}]
                         (let [new-key (images/insert-image id filename tempfile)
                               new-url (format "/documents/%s/images/%s" id new-key)]
                           (created new-url)))}}]
     ["/:image-id"
      {:get {:summary "Get an image"
             :parameters {:path {:id int? :image-id int?}}
             :handler (fn [{{{:keys [id image-id]} :path} :parameters}]
                        (if-let [image (images/get-image id image-id)]
                          (ok image)
                          (not-found)))}
       :delete {:summary "Delete an image"
                :middleware [wrap-restricted]
                :swagger {:security [{:apiAuth []}]}
                :parameters {:path {:id int? :image-id int?}}
                :handler (fn [{{{:keys [id image-id]} :path} :parameters}]
                           (let [deleted (images/delete-image id image-id)]
                             (case deleted
                               true (no-content)
                               false (internal-server-error) ; we found something but could not delete the content
                               nil (not-found))))}}]]]

   ["/confirmable"
    {:swagger {:tags ["Confirmable Words"]}
     :get {:summary "Get all local words that are ready to be confirmed"
           :parameters {:query {(spec/opt :limit) int?
                                (spec/opt :offset) int?}}
           :handler (fn [{{{:keys [limit offset]
                            :or {limit default-limit offset 0}} :query} :parameters}]
                      (ok (confirm/get-words limit offset)))}

     :put {:summary "Confirm a local word"
           :middleware [wrap-restricted wrap-authorized]
           :swagger {:security [{:apiAuth []}]}
           :parameters {:body {:untranslated string? :type int?
                               :uncontracted ::braille
                               :contracted ::braille
                               :homograph-disambiguation string?
                               :document-id int? :islocal boolean?
                               :hyphenated (spec/maybe ::hyphenation)
                               :spelling ::spelling}}
           :handler (fn [{{word :body} :parameters}]
                      (let [modified (confirm/put-word word)]
                        (if (> modified 0)
                          (no-content)
                          (not-found))))}}]

   ["/hyphenations"
    {:swagger {:tags ["Hyphenations"]}}

    [""
     {:get {:summary "Get hyphenations by spelling. Optionally filter the results by using a search string, a limit and an offset."
            :parameters {:query {:spelling ::spelling
                                 (spec/opt :search) string?
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [spelling search limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (ok (db/get-hyphenation {:spelling spelling :search (db/search-to-sql search)
                                                :limit limit :offset offset})))}

      :put {:summary "Update or create a hyphenation"
            :middleware [wrap-restricted]
            :swagger {:security [{:apiAuth []}]}
            :parameters {:body {:word string?
                                :hyphenation string?
                                :spelling ::spelling}}
            :handler (fn [{{{:keys [word hyphenation spelling]} :body} :parameters}]
                       (db/insert-hyphenation {:word word :hyphenation hyphenation :spelling spelling})
                       (no-content))}

      :delete {:summary "Delete a hyphenation"
               :middleware [wrap-restricted]
               :swagger {:security [{:apiAuth []}]}
               :parameters {:body {:word string?
                                   :spelling ::spelling
                                   :hyphenation string?}}
               :handler (fn [{{{:keys [word spelling]} :body} :parameters}]
                          (let [deleted (db/delete-hyphenation {:word word :spelling spelling})]
                            (if (> deleted 0)
                              (no-content) ; we found something and deleted it
                              ;; if there was no deletion it can mean that either the hyphenation doesn't
                              ;; exist or and this is more likely that the associated word still exists in
                              ;; either the global or the local table. In that case respond with
                              ;; precondition-failed.
                              (precondition-failed {:status-text "The hyphenation either doesn't exist or more likely there is still an associated word in the local or in the global database."}))))}}]
    ["/suggested"
      {:get {:summary "Get the suggested hyphenation for a given word and spelling"
             :parameters {:query {:spelling ::spelling
                                  :word string?}}
             :handler (fn [{{{:keys [word spelling]} :query} :parameters}]
                        (ok {:hyphenation (hyphenate/hyphenate word spelling)}))}}]]
   ])
