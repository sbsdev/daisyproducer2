(ns daisyproducer2.pipeline2.core
  "Thin layer on top of the [Pipeline2 Web Service
  API](https://daisy.github.io/pipeline/WebServiceAPI)"
  (:require
   [babashka.fs :as fs]
   [clj-http.client :as client]
   [clj-http.util :refer [url-encode]]
   [clojure.data.codec.base64 :as b64]
   [clojure.data.xml :as xml]
   [clojure.data.zip :as zf]
   [clojure.data.zip.xml :refer [attr xml-> xml1->]]
   [clojure.java.io :as io]
   [clojure.zip :refer [xml-zip]]
   [crypto.random :as crypt-rand]
   [daisyproducer2.config :refer [env]]
   [java-time :as time]
   [pandect.algo.sha1 :as pandect]))

;; the following assumes that the pipeline is run in remote mode and
;; the default webservice url

(def ws-url "http://localhost:8181/ws")

(def ^:private timeout 1000)
(def ^:private poll-interval 3000)

(defn- create-hash [message signing-key]
  (-> (pandect/sha1-hmac-bytes message signing-key)
      b64/encode
      String.))

(defn- auth-query-params [uri]
  (if (get-in env [:pipeline2 :authentication])
    (let [timestamp (time/format :iso-local-date-time (time/local-date-time))
          nonce (crypt-rand/base64 32)
          auth-id (get-in env [:pipeline2 :auth-id])
          params {"authid" auth-id "time" timestamp "nonce" nonce}
          query-string (str uri "?" (client/generate-query-string params))
          hashcode (create-hash query-string (get-in env [:pipeline2 :secret]))]
      {:query-params
       {"authid" auth-id "time" timestamp "nonce" nonce "sign" hashcode}})
    {}))

(def qname (partial xml/qname "http://www.daisy.org/ns/pipeline/data"))

(defn- job-sexp [script input options]
  (let [script-url (str ws-url "/scripts/" script)]
    [(qname "jobRequest")
     [(qname "script") {:href script-url}]
     [(qname "input") {:name "source"}
      (for [item input]
        [(qname "item") {:value (url-encode (fs/file-name (fs/file item)))}])]
     (for [[key value] options]
       [(qname "option") {:name (name key)} value])]))

(defn- job-request [script input options]
  (-> (job-sexp script input options)
      xml/sexp-as-element
      xml/emit-str))

(defn jobs []
  (let [url (str ws-url "/jobs")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn get-job [id]
  (let [url (str ws-url "/jobs/" id)
        response (client/get url (merge (auth-query-params url)
                                        #_{:socket-timeout timeout :conn-timeout timeout}))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn- zip-files [files]
  (let [tmp-name (str (fs/create-temp-file {:prefix "pipeline2-client" :suffix ".zip"}))]
    (fs/zip tmp-name (map #(fs/relativize (fs/cwd) %) files) {:path-fn fs/file-name})
    tmp-name))

(defn- multipart-request [input body]
  {:multipart
   [{:name "job-data" :content (io/file (zip-files input))}
    {:name "job-request" :content body}]})

(defn job-create [script input data options]
  (let [url (str ws-url "/jobs")
        request (job-request script input options)
        auth (auth-query-params url)
        multipart (multipart-request (concat input data) request)
        response (client/post url (merge multipart auth))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn- job-result-1 [url]
  (let [response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn job-result
  ([id]
     (job-result-1 (str ws-url "/jobs/" id "/result")))
  ([id type name]
     (job-result-1 (str ws-url "/jobs/" id "/result/" type "/" name)))
  ([id type name idx]
     (job-result-1 (str ws-url "/jobs/" id "/result/" type "/" name "/idx/" idx))))

(defn job-log [id]
  (let [url (str ws-url "/jobs/" id "/log")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body))))

(defn job-delete [id]
  (let [url (str ws-url "/jobs/" id)
        response (client/delete url (auth-query-params url))]
    (client/success? response)))

(defn scripts []
  (let [url (str ws-url "/scripts")
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn script [id]
  (let [url (str ws-url "/scripts/" id)
        response (client/get url (auth-query-params url))]
    (when (client/success? response)
      (-> response :body xml/parse-str))))

(defn alive? []
  (let [url (str ws-url "/alive")]
    (client/success? (client/get url))))

(defn get-id [job]
  (-> job xml-zip (xml1-> (attr :id))))

(defn- get-status [job]
  (-> job xml-zip (xml1-> (attr :status))))

(defn get-results [job]
  ;; unfortunately `(xml-> :results :result :result (attr :href))`
  ;; doesn't work here as `xml->` has a problem with that, see
  ;; https://dev.clojure.org/jira/browse/DZIP-6
  (-> job xml-zip (xml-> (qname "results") (qname "result") zf/children (attr :href))))

(defn get-stream [url]
  (let [response (client/get url (merge (auth-query-params url) {:as :stream}))]
    (when (client/success? response)
      (-> response :body))))

(defmacro with-job
  [[job job-create-form] & body]
  `(let [~job ~job-create-form]
     (try
       ~@body
       (catch Exception e#
         (throw (ex-info
                 (format "Failed to run Pipeline2 job because %s" (ex-message e#))
                 {:error-id ::pipeline2-failure})))
       (finally
         (when ~job
           (job-delete (get-id ~job)))))))

(defn wait-for [job]
  (Thread/sleep poll-interval) ; wait a bit before polling the first time
  (let [id (get-id job)]
    (loop [result (get-job id)]
      (let [status (get-status result)]
        (if (= "RUNNING" status)
          (do
            (Thread/sleep poll-interval)
            (recur (get-job id)))
          result)))))

(defn create-job-and-wait [script input data options]
  (-> (job-create script input data options)
      wait-for))
