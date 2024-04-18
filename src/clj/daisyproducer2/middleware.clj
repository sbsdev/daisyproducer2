(ns daisyproducer2.middleware
  (:require
    [daisyproducer2.env :refer [defaults]]
    [clojure.set :refer [intersection]]
    [clojure.tools.logging :as log]
    [daisyproducer2.layout :refer [error-page]]
    [daisyproducer2.i18n :refer [tr]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [daisyproducer2.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [daisyproducer2.config :refer [env]]
    [daisyproducer2.auth :as auth]
    [reitit.ring :as ring]
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.util.http-response :refer [unauthorized forbidden]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [buddy.auth.middleware :refer [wrap-authentication wrap-authorization]]
    [buddy.auth.accessrules :refer [restrict]]
    [buddy.auth :refer [authenticated?]]
    [buddy.auth.backends.token :refer [jws-backend]]
    [buddy.auth.backends.session :refer [session-backend]]
    [iapetos.collector.ring :as prometheus]
    [daisyproducer2.metrics :as metrics])
  )

(defn wrap-internal-error [handler]
  (let [error-result (fn [^Throwable t]
                       (log/error t (.getMessage t))
                       (error-page {:status 500
                                    :title (tr [:something-bad-happened])
                                    :message (tr [:something-bad-happened-message])}))]
    (fn wrap-internal-error-fn
      ([req respond _]
       (handler req respond #(respond (error-result %))))
      ([req]
       (try
         (handler req)
         (catch Throwable t
           (error-result t)))))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title (tr [:invalid-anti-forgery-token])})}))


(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn
      ([request]
         ;; disable wrap-formats for websockets
         ;; since they're not compatible with this middleware
       ((if (:websocket? request) handler wrapped) request))
      ([request respond raise]
       ((if (:websocket? request) handler wrapped) request respond raise)))))

(defn on-unauthenticated [request response]
  (unauthorized
   {:status-text (tr [:not-authenticated] [(:uri request)])}))

(defn on-unauthorized [request response]
  (forbidden
   {:status-text (tr [:not-authorized] [(:uri request)])}))

;; https://learning.oreilly.com/library/view/web-development-with/9781680508833/f_0048.xhtml#:-:text=get-roles-from-match
(defn get-roles-from-match [request]
  (let [request-method (:request-method request)]
    (-> request (ring/get-match) (get-in [:data request-method :authorized] #{}))))

(defn- get-roles-from-identity [request]
  (->> (get-in request [:identity :user :roles])
       ;; unfortunately the jws-backend parses the token as json and
       ;; hence ignores keywords and sets. So we have to reconstruct
       ;; those here
       (map keyword)
       set))

(defn authorized?
  "Return true if the roles of the identity in `request` intersect with
  the `roles` defined in the router. So if an identity has no roles it
  will never be authorized."
  [request]
  (let [route-roles (get-roles-from-match request)
        request-roles (get-roles-from-identity request)]
    (some? (seq (intersection route-roles request-roles)))))

(defn wrap-authorized [handler]
  (restrict handler {:handler authorized?
                     :on-error on-unauthorized})  )

(defn wrap-restricted [handler]
  (restrict handler {:handler authenticated?
                     :on-error on-unauthenticated}))

;; see https://adambard.com/blog/clojure-auth-with-buddy/ for some
;; inspiration on how to do JW* Token Authentication
(defn wrap-auth [handler]
  (let [backend (jws-backend {:secret (env :jwt-secret)})]
    (-> handler
        (wrap-authentication backend)
        (wrap-authorization backend))))

(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      wrap-auth
      wrap-flash
      (wrap-session {:cookie-attrs {:http-only true}})
      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (dissoc :session)))
      (prometheus/wrap-metrics
       metrics/registry {:path "/metrics"})
      wrap-internal-error))
