(ns daisyproducer2.routes.services
  (:require
   [babashka.fs :as fs]
   [clojure.spec.alpha :as s]
   [clojure.string :as str :refer [blank?]]
   [clojure.tools.logging :as log]
   [daisyproducer2.auth :as auth]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.db.core :as db]
   [daisyproducer2.documents.abacus :as abacus]
   [daisyproducer2.documents.alfresco.core :as alfresco]
   [daisyproducer2.documents.documents :as documents]
   [daisyproducer2.documents.images :as images]
   [daisyproducer2.documents.preview :as preview]
   [daisyproducer2.documents.products :as products]
   [daisyproducer2.documents.versions :as versions]
   [daisyproducer2.hyphenate :as hyphenate]
   [daisyproducer2.hyphenations :as hyphenations]
   [daisyproducer2.middleware :refer [wrap-authorized wrap-restricted]]
   [daisyproducer2.middleware.exception :as exception]
   [daisyproducer2.middleware.formats :as formats]
   [daisyproducer2.validation :as validation]
   [daisyproducer2.words.confirm :as confirm]
   [daisyproducer2.words.global :as global]
   [daisyproducer2.words.local :as local]
   [daisyproducer2.words.unknown :as unknown]
   [failjure.core :as f]
   [reitit.coercion.spec :as spec-coercion]
   [reitit.ring.coercion :as coercion]
   [reitit.ring.middleware.multipart :as multipart]
   [reitit.ring.middleware.muuntaja :as muuntaja]
   [reitit.ring.middleware.parameters :as parameters]
   [reitit.swagger :as swagger]
   [reitit.swagger-ui :as swagger-ui]
   [ring.util.http-response :refer :all]
   [spec-tools.data-spec :as spec]))

(s/def ::grade (s/and int? #(<= 0 % 2)))
(s/def ::type (s/and int? #(<= 0 % 5)))
(s/def ::spelling (s/and int? #{0 1}))
(s/def ::braille (s/and string? validation/braille-valid?))
(s/def ::hyphenation (s/and string? validation/hyphenation-valid?))
(s/def ::state #{"open" "closed"})
(s/def ::accented-chars (s/and keyword? #{:basic :swiss}))
(s/def ::footnote-placement (s/and keyword? #{:standard :end-vol :level1 :level2 :level3 :level4}))
(s/def ::font-size (s/and int? #{17 20 25}))
(s/def ::font (s/and keyword? #{:tiresias :roman :sans :mono}))
(s/def ::page-style (s/and keyword? #{:plain :with-page-nums :spacious :scientific}))
(s/def ::alignment (s/and keyword? #{:left :justified}))
(s/def ::stock-size (s/and keyword? #{:a3paper :a4paper}))
(s/def ::line-spacing (s/and keyword? #{:singlespacing :onehalfspacing :doublespacing}))
(s/def ::end-notes (s/and keyword? #{:none :document :chapter}))
(s/def ::image-visibility (s/and keyword? #{:show :ignore}))
(s/def ::asciimath (s/and keyword? #{:asciimath :mathml :both}))
(s/def ::image-handling (s/and keyword? #{:embed :link :drop}))
(s/def ::product-type (s/and int? #(<= 0 % 3)))
;; (s/and keyword? #{:braille :large-print :ebook :etext}))
(s/def ::product-number (s/and string? #(re-matches #"^(PS|GD|EB|ET)\d{4,7}$" %)))

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
                             (documents/get-documents limit offset)
                             (documents/find-documents limit offset (db/search-to-sql search)))))}}]

    ["/:id"
     [""
      {:get {:summary "Get a document by ID"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (ok doc)
                          (not-found)))}

       :patch {:summary "Patch a document, e.g. update the state"
               :middleware [wrap-restricted wrap-authorized]
               :swagger {:security [{:apiAuth []}]}
               :authorized #{:admin :review}
               :parameters {:path {:id int?}
                            :body {:state ::state}}
               :handler (fn [{{{:keys [id]} :path {:keys [state]} :body} :parameters}]
                          (let [doc (documents/get-document id)]
                            (cond
                              (nil? doc) (not-found)
                              (= (:state doc) state)
                              (conflict {:status-text (if (= state "closed") "Cannot close a closed production" "Cannot reopen an open production")})
                              :else (try
                                      (db/update-document-state {:id id :state state})
                                      (no-content)
                                      (catch clojure.lang.ExceptionInfo e
                                        (log/error (ex-message e))
                                        (internal-server-error {:status-text (ex-message e)}))))))}

       :delete {:summary "Delete a document including associated words, versions, images and products"
               :middleware [wrap-restricted wrap-authorized]
               :swagger {:security [{:apiAuth []}]}
               :authorized #{:admin}
                :parameters {:path {:id int?}}
               :handler (fn [{{{:keys [id]} :path} :parameters}]
                          (let [deleted (documents/delete-document id)]
                            (if (pos? deleted)
                              (no-content)
                              (not-found))))}}]]]

   ["/words"
    {:swagger {:tags ["Global Words"]}}

    [""
     {:get {:summary "Get global words. Optionally filter the results by using `search`, `limit` and `offset`."
            :parameters {:query {(spec/opt :search) string?
                                 (spec/opt :type) ::type
                                 (spec/opt :limit) int?
                                 (spec/opt :offset) int?}}
            :handler (fn [{{{:keys [search type limit offset]
                             :or {limit default-limit offset 0}} :query} :parameters}]
                       (ok (if (blank? search)
                             (global/get-words {:type type :limit limit :offset offset})
                             (global/find-words {:search search :type type :limit limit :offset offset}))))}

      :put {:summary "Update or create a global word"
            :middleware [wrap-restricted wrap-authorized]
            :swagger {:security [{:apiAuth []}]}
            :authorized #{:admin}
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
               :authorized #{:admin}
               :parameters {:body {:untranslated string?
                                   :type int?
                                   :uncontracted ::braille
                                   :contracted ::braille
                                   :homograph-disambiguation string?}}
               :handler (fn [{{word :body} :parameters}]
                          (let [deleted (global/delete-word word)]
                            (if (pos? deleted)
                              (no-content)
                              (not-found))))}}]]

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

      :head {:summary "Get the total number of local words for a given document. The total is returned as a special Response Header \"X-Result-Count\""
            :parameters {:path {:id int?}
                         :query {:grade ::grade}}
            :handler (fn [{{{:keys [id]} :path
                            {:keys [grade]} :query} :parameters}]
                       (let [{total :total} (db/get-all-local-words-total {:document-id id :grade grade})]
                         (-> {} ok (header "X-Result-Count" total))))}

      :put {:summary "Update or create a local word for a given document"
            :middleware [wrap-restricted]
            :swagger {:security [{:apiAuth []}]}
            :authorized #{:user :admin}
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
               :authorized #{:user :admin}
               :parameters {:body {:untranslated string? :type int?
                                   (spec/opt :uncontracted) ::braille
                                   (spec/opt :contracted) ::braille
                                   :homograph-disambiguation string?
                                   :document-id int?
                                   :hyphenated (spec/maybe ::hyphenation)
                                   :spelling ::spelling}}
               :handler (fn [{{word :body} :parameters}]
                          (let [deleted (local/delete-word word)]
                            (if (pos? deleted)
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
                       (let [unknown (unknown/get-words id grade limit offset)]
                         (ok unknown)))}
      :head {:summary "Get the total number of unknown words for a given document. The total is returned as a special Response Header \"X-Result-Count\""
            :parameters {:path {:id int?}
                         :query {:grade ::grade}}
            :handler (fn [{{{:keys [id]} :path
                            {:keys [grade]} :query} :parameters}]
                       (let [{total :total} (db/get-all-unknown-words-total {:document-id id :grade grade})]
                         (-> {} ok (header "X-Result-Count" total))))}

      :put {:summary "Update an unknown word"
            :middleware [wrap-restricted]
            :swagger {:security [{:apiAuth []}]}
            :authorized #{:user :admin}
            :parameters {:body {:untranslated string?
                                :type ::type
                                :homograph-disambiguation string?
                                :document-id int?
                                :islocal boolean?
                                :isignored boolean?}}
           :handler (fn [{{word :body} :parameters}]
                      (let [modified (unknown/put-word word)]
                        (if (pos? modified)
                          (no-content)
                          (not-found))))}}]

    ["/preview"
     {:swagger {:tags ["Preview"]}}

     ["/epub"
      {:get {:summary "Get an EPUB file for a document by ID"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [[name path] (preview/epub id)]
                              ;; copy the result to a spool dir for the online player if there is a
                              ;; spool dir defined for the language and if the epub name contains a
                              ;; valid product-id
                              (if-let [spool-dir (get-in env [:ebook-spool-dir (:language doc)])]
                                (let [product-id (str/replace name #".epub$" "")]
                                  (when (s/valid? ::product-number product-id)
                                    (fs/copy path spool-dir {:replace-existing true}))))
                              (let [url (format "/download/%s/epub/%s" id name)]
                                (created url {:location url})))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]

     ["/epub-in-player"
      {:get {:summary "Generate the EPUB and redirect to a view of it in the online player"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [version-id (-> (versions/get-latest id) :id)
                                  spool-dir (get-in env [:online-player :spool-dir])]
                              ;; use the cached version if it exists
                              (when-not (fs/exists? (fs/path spool-dir (str version-id) "EPUB" "package.opf"))
                                (let [[_ path] (preview/epub id version-id spool-dir)]
                                    ;; unpack it in the spool directory
                                    (fs/unzip path (fs/path spool-dir (str version-id)) {:replace-existing true})
                                    ;; remove the epub (as we only need the unpacked artifact)
                                    (fs/delete path)))
                              (let [player-url (get-in env [:online-player :url])
                                    source (format (get-in env [:online-player :source]) version-id)
                                    url (str player-url source)]
                                (created url {:location url})))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]

     ["/braille"
      {:get {:summary "Get a braille file for a document by ID"
             :parameters {:path {:id int?}
                          :query {:contraction ::grade
                                  :cells-per-line int?
                                  :lines-per-page int?
                                  :hyphenation boolean?
                                  :toc-level int?
                                  :footer-level int?
                                  :include-macros boolean?
                                  :show-original-page-numbers boolean?
                                  :show-v-forms boolean?
                                  :downshift-ordinals boolean?
                                  :enable-capitalization boolean?
                                  :detailed-accented-characters ::accented-chars
                                  :footnote-placement ::footnote-placement}}
             :handler (fn [{{{:keys [id]} :path
                             opts :query} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [[name _] (preview/sbsform doc opts)
                                  contraction (:contraction opts)
                                  url (format "/download/%s/braille/%s/%s" id contraction name)]
                              (created url {:location url}))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]

     ["/large-print"
      {:get {:summary "Get a large print file for a document by ID"
             :parameters {:path {:id int?}
                          :query {(spec/opt :font-size) ::font-size
                                  (spec/opt :font) ::font
                                  (spec/opt :page-style) ::page-style
                                  (spec/opt :alignment) ::alignment
                                  (spec/opt :stock-size) ::stock-size
                                  (spec/opt :line-spacing) ::line-spacing
                                  (spec/opt :replace-em-with-quote) boolean?
                                  (spec/opt :end-notes) ::end-notes
                                  (spec/opt :image-visibility) ::image-visibility}}
             :handler (fn [{{{:keys [id]} :path
                             opts :query} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [[name _] (preview/large-print id opts)
                                  font-size (or (:font-size opts) 17)
                                  url (format "/download/%s/large-print/%s/%s" id font-size name)]
                              (created url {:location url}))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]

     ["/open-document"
      {:get {:summary "Get an OpenDocument Text Document (ODT) for a document by ID"
             :parameters {:path {:id int?}
                          :query {(spec/opt :math) ::asciimath
                                  (spec/opt :phonetics) boolean?
                                  (spec/opt :image-handling) ::image-handling
                                  (spec/opt :line-numbers) boolean?
                                  (spec/opt :page-numbers) boolean?
                                  (spec/opt :page-numbers-float) boolean?
                                  (spec/opt :answer) string?}}
             :handler (fn [{{{:keys [id]} :path
                             opts :query} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [[name _] (preview/open-document id opts)
                                  url (format "/download/%s/open-document/%s" id name)]
                              (created url {:location url}))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]

     ["/html"
      {:get {:summary "Get an HTML for a document by ID"
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters}]
                        (if-let [doc (documents/get-document id)]
                          (try
                            (let [[name _] (preview/html id)
                                  url (format "/download/%s/html/%s" id name)]
                              (created url {:location url}))
                            (catch clojure.lang.ExceptionInfo e
                              (log/error (ex-message e))
                              (internal-server-error {:status-text (ex-message e)}))
                            (catch java.nio.file.FileSystemException e
                              (log/error (str e))
                              (internal-server-error {:status-text (str e)})))
                          (not-found)))}}]]

    ["/versions"
     {:swagger {:tags ["Versions"]}}

     [""
      {:get {:summary "Get all versions of a given document. If `latest` is true, return only the latest version. Optionally limit the result set using a `search` term, a `limit` and an `offset`."
             :parameters {:path {:id int?}
                          :query {(spec/opt :latest) boolean?
                                  (spec/opt :search) string?
                                  (spec/opt :limit) int?
                                  (spec/opt :offset) int?}}
             :handler (fn [{{{:keys [id]} :path
                             {:keys [limit offset search latest]
                              :or {limit default-limit offset 0 latest false}} :query} :parameters}]
                        (if latest
                          (if-let [latest (versions/get-latest id)]
                            (ok latest)
                            (not-found))
                          (let [versions (if (blank? search)
                                           (versions/get-versions id limit offset)
                                           (versions/find-versions  id limit offset (db/search-to-sql search)))]
                            (ok versions))))}
       :post {:summary "Create a new version for a given document"
              :middleware [wrap-restricted]
              :swagger {:security [{:apiAuth []}]}
              :authorized #{:user :admin}
              :parameters {:path {:id int?}
                           :multipart {:file multipart/temp-file-part
                                       :comment string?}}
              :handler (fn [{{{:keys [id]} :path {{tempfile :tempfile} :file comment :comment} :multipart} :parameters
                             {{uid :uid} :user} :identity}]
                         (try
                           (let [new-key (versions/insert-version id tempfile comment uid)]
                             (if (f/ok? new-key)
                               ;; the uploaded xml is valid
                               (let [new-url (format "/documents/%s/versions/%s" id new-key)]
                                 ;; update the unknown words list for this document
                                 (unknown/update-words tempfile id)
                                 (created new-url {})) ;; add an empty body
                               ;; the uploaded xml contains errors
                               (let [message (f/message new-key)
                                     errors (:errors new-key)]
                                 (log/warn message errors)
                                 (bad-request {:status-text message :errors errors}))))
                           (catch clojure.lang.ExceptionInfo e
                             (let [{:keys [error-id errors]} (ex-data e)
                                   message (ex-message e)]
                               (log/error message error-id errors)
                               (internal-server-error {:status-text (ex-message e) :errors errors})))
                           (finally
                             (fs/delete tempfile))))}}]

     ["/:version-id"
      {:get {:summary "Get a version"
              :parameters {:path {:id int? :version-id int?}}
              :handler (fn [{{{:keys [id version-id]} :path} :parameters}]
                         (if-let [version (versions/get-version version-id)]
                           (ok version)
                           (not-found)))}
        :delete {:summary "Delete a version"
                 :middleware [wrap-restricted]
                 :swagger {:security [{:apiAuth []}]}
                 :authorized #{:admin}
                 :parameters {:path {:id int? :version-id int?}}
                 :handler (fn [{{{:keys [id version-id]} :path} :parameters}]
                            (let [deleted (versions/delete-version id version-id)]
                              (if (pos? deleted)
                                (no-content) ; we found something and deleted it
                                (not-found))))}}]]

    ["/images"
     {:swagger {:tags ["Images"]}}

     [""
      {:get {:summary "Get all images of a given document. Optionally limit the result set using a `search` term, a `limit` and an `offset`."
             :parameters {:path {:id int?}
                          :query {(spec/opt :search) string?
                                  (spec/opt :limit) int?
                                  (spec/opt :offset) int?}}
             :handler (fn [{{{:keys [id]} :path
                             {:keys [limit offset search]} :query} :parameters}]
                        (ok (if (blank? search)
                              (images/get-images id limit offset)
                              (images/find-images id limit offset (db/search-to-sql search)))))}
       :post {:summary "Add a new image to a given document"
              :middleware [wrap-restricted]
              :swagger {:security [{:apiAuth []}]}
              :authorized #{:user :admin}
              :parameters {:path {:id int?} :multipart {:file multipart/temp-file-part}}
              :handler (fn [{{{:keys [id]} :path {{:keys [filename tempfile]} :file} :multipart} :parameters
                             {{uid :uid} :user} :identity}]
                         (try
                           (if-let [new-key (images/insert-image id filename tempfile)]
                             ;; a new image resource was created
                             (let [new-url (format "/documents/%s/images/%s" id new-key)]
                               (created new-url {}))
                             ;; an existing image was uploaded again
                             (no-content))
                           (finally
                             (fs/delete tempfile))))}
       :delete {:summary "Delete all images of a given document"
                :middleware [wrap-restricted]
                :swagger {:security [{:apiAuth []}]}
                :authorized #{:user :admin}
                :parameters {:path {:id int?}}
                :handler (fn [{{{:keys [id]} :path} :parameters
                               {{uid :uid} :user} :identity}]
                           (let [deleted (images/delete-all-images id)]
                             (if (pos? deleted)
                              (ok {:deleted deleted}) ; we found something and deleted it
                              (not-found))))}}]
     ["/:image-id"
      {:get {:summary "Get an image"
             :parameters {:path {:id int? :image-id int?}}
             :handler (fn [{{{:keys [id image-id]} :path} :parameters}]
                        (if-let [image (images/get-image image-id)]
                          (ok image)
                          (not-found)))}
       :delete {:summary "Delete an image"
                :middleware [wrap-restricted]
                :swagger {:security [{:apiAuth []}]}
                :authorized #{:user :admin}
                :parameters {:path {:id int? :image-id int?}}
                :handler (fn [{{{:keys [id image-id]} :path} :parameters}]
                           (let [deleted (images/delete-image id image-id)]
                             (if (pos? deleted)
                              (no-content) ; we found something and deleted it
                              (not-found))))}}]]
    ["/products"
     {:swagger {:tags ["Products"]}}

     [""
      {:get {:summary "Get all products of a given document. Optionally limit the result by `type`."
             :parameters {:path {:id int?}
                          :query {(spec/opt :type) ::product-type}}
             :handler (fn [{{{:keys [id]} :path
                             {:keys [type]} :query} :parameters}]
                        (ok (if type
                              (products/get-products id type)
                              (products/get-products id))))}
       :post {:summary "Add a new product to a given document"
              :middleware [wrap-restricted]
              :swagger {:security [{:apiAuth []}]}
              :authorized #{:admin}
              :parameters {:path {:id int?}
                           :body {:product-number ::product-number
                                  :type ::product-type}}
              :handler (fn [{{{:keys [id]} :path {:keys [product-number type]} :body} :parameters}]
                         (try
                           (let [new-key (products/insert-product id product-number type)
                                 new-url (format "/documents/%s/products/%s" id new-key)]
                             (created new-url {}))
                           (catch clojure.lang.ExceptionInfo e
                             (log/warn (ex-message e))
                             (bad-request {:status-text (ex-message e)}))))}}]
     ["/:product-id"
      {:get {:summary "Get a product"
             :parameters {:path {:id int? :product-id int?}}
             :handler (fn [{{{:keys [id product-id]} :path} :parameters}]
                        (if-let [product (products/get-product product-id)]
                          (ok product)
                          (not-found)))}
       :delete {:summary "Delete a product"
                :middleware [wrap-restricted]
                :swagger {:security [{:apiAuth []}]}
                :authorized #{:admin}
                :parameters {:path {:id int? :product-id int?}}
                :handler (fn [{{{:keys [product-id]} :path} :parameters}]
                           (let [deleted (products/delete-product product-id)]
                             (if (pos? deleted)
                              (no-content) ; we found something and deleted it
                              (not-found))))}}]]]

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
           :authorized #{:admin}
           :parameters {:body {:untranslated string? :type int?
                               :uncontracted ::braille
                               :contracted ::braille
                               :homograph-disambiguation string?
                               :document-id int? :islocal boolean?
                               :hyphenated (spec/maybe ::hyphenation)
                               :spelling ::spelling}}
           :handler (fn [{{word :body} :parameters}]
                      (let [modified (confirm/put-word word)]
                        (if (pos? modified)
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
                       (ok (hyphenations/get-hyphenations spelling search limit offset)))}

      :put {:summary "Update or create a hyphenation"
            :middleware [wrap-restricted]
            :swagger {:security [{:apiAuth []}]}
            :authorized #{:user :admin}
            :parameters {:body {:word string?
                                :hyphenation string?
                                :spelling ::spelling}}
            :handler (fn [{{{:keys [word hyphenation spelling]} :body} :parameters}]
                       (hyphenations/put-hyphenation word hyphenation spelling)
                       (no-content))}

      :delete {:summary "Delete a hyphenation"
               :middleware [wrap-restricted]
               :swagger {:security [{:apiAuth []}]}
               :authorized #{:user :admin}
               :parameters {:body {:word string?
                                   :spelling ::spelling
                                   :hyphenation string?}}
               :handler (fn [{{{:keys [word spelling]} :body} :parameters}]
                          (let [deleted (hyphenations/delete-hyphenation word spelling)]
                            (if (pos? deleted)
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

   ["/abacus"
    {:swagger {:tags ["Abacus API"]}}

    ["/import"
     {:post {:summary "Add a production"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :handler (fn [{{{:keys [file]} :multipart} :parameters}]
                        (try
                          (let [tempfile (:tempfile file)
                                import (abacus/read-file tempfile)
                                {:keys [document-id status]} (abacus/import-new-document import)]
                            (case status
                              :ignored (unprocessable-entity {:status :ignored :message "Document not for Daisyproducer"})
                              :updated (ok {:updated document-id})
                              :created (let [url (str "/api/documents/" document-id)]
                                         (created url {:location url}))))
                          (catch clojure.lang.ExceptionInfo e
                            (let [{:keys [error-id errors document]} (ex-data e)
                                  message (ex-message e)
                                  {:keys [filename tempfile]} file]
                              (if (= error-id :input-ignored)
                                (let [{:keys [product-number title]} document]
                                  (log/infof "Ignoring %s (%s)" product-number title))
                                (log/error message error-id errors filename (str tempfile)))
                              (case error-id
                                :input-ignored (no-content)
                                :duplicate-key (bad-request {:status-text message})
                                :invalid-xml (bad-request {:status-text "Upload of ABACUS XML failed"
                                                           :error-id error-id :errors errors})
                                :invalid-isbn (bad-request {:status-text "The ISBN is not valid"
                                                            :error-id error-id :errors errors})
                                :invalid-product-number (bad-request {:status-text "The product number is not valid"
                                                                      :error-id error-id :errors errors})
                                (internal-server-error {:status-text message}))))))}}]]
   ["/alfresco"
    {:swagger {:tags ["Alfresco API"]}}
    ["/:id"
     {:get {:summary "Check whether a production has been archived before"
            :parameters {:path {:id int?}}
            :handler (fn [{{{:keys [id]} :path} :parameters}]
                       (if-let [doc (documents/get-document id)]
                         (if (alfresco/archived? doc)
                           (ok {})
                           (not-found))
                         (not-found)))}

      ;; see https://stackoverflow.com/q/16877968 for a discussion on which HTTP verb to use for
      ;; this more rpc like end point
      :post {:summary "Update a production with content from the archive"
             :middleware [wrap-restricted]
             :swagger {:security [{:apiAuth []}]}
             :authorized #{:user :admin}
             :parameters {:path {:id int?}}
             :handler (fn [{{{:keys [id]} :path} :parameters
                            {{uid :uid} :user} :identity}]
                        (try
                          (if-let [doc (documents/get-document id)]
                            (if (alfresco/archived? doc)
                              (let [result (alfresco/synchronize doc uid)]
                                (if (f/ok? result)
                                  ;; the xml from the archive is valid
                                  (no-content)
                                  ;; the xml from the archive contains errors
                                  (let [message (f/message result)
                                        errors (:errors result)]
                                    (log/warn message errors)
                                    (conflict {:status-text "DTBook XML from archive not valid" :errors errors}))))
                              (not-found {:errors ["No archived version of document was found"]}))
                            (not-found))
                          (catch clojure.lang.ExceptionInfo e
                            (let [{:keys [error-id errors document]} (ex-data e)
                                  message (ex-message e)]
                              (log/error message error-id errors)
                              (internal-server-error {:status-text message})))
                          (catch java.nio.file.FileSystemException e
                            (log/error (str e))
                            (internal-server-error {:status-text (str e)}))))}}]]
   ])
