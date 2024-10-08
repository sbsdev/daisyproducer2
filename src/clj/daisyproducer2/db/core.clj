(ns daisyproducer2.db.core
  (:require
    [next.jdbc.date-time]
    [next.jdbc.result-set]
    [clojure.string :as str]
    [clojure.tools.logging :as log]
    [conman.core :as conman]
    [camel-snake-kebab.extras :refer [transform-keys]]
    [camel-snake-kebab.core :refer [->kebab-case-keyword]]
    [daisyproducer2.config :refer [env]]
    [mount.core :refer [defstate]]))

(defstate ^:dynamic *db*
  :start (if-let [jdbc-url (env :database-url)]
           (conman/connect! {:jdbc-url jdbc-url})
           (do
             (log/warn "database connection URL was not found, please set :database-url in your config, e.g: dev-config.edn")
             *db*))
  :stop (conman/disconnect! *db*))

(conman/bind-connection *db* "sql/queries.sql")

(defn search-to-sql
  "Prepare given search string `s` for search in SQL. If the string
  neither starts with '^' nor ends with '$' then it is simply wrapped
  in '%'. If it starts with '^' or ends with '$' then the respective
  '%' is not added."
  [s]
  (let [prepend #(str %2 %1)
        append #(str %1 %2)]
    (cond-> s
      (not (str/starts-with? s "^")) (prepend "%")
      (not (str/ends-with? s "$")) (append "%")
      true (str/replace #"[$^]" ""))))

(defn get-generated-key
  "Extract the generated key from the return value of an insert statement.
  NOTE: This most likely only works when using MySQL and `clojure.jdbc`,
  see also the [section on Retrieving Last Inserted ID](https://www.hugsql.org/#using-insert)"
  [return-value]
  (-> return-value first :generated_key))

(extend-protocol next.jdbc.result-set/ReadableColumn
  java.sql.Timestamp
  (read-column-by-label [^java.sql.Timestamp v _]
    (.toLocalDateTime v))
  (read-column-by-index [^java.sql.Timestamp v _2 _3]
    (.toLocalDateTime v))
  java.sql.Date
  (read-column-by-label [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index [^java.sql.Date v _2 _3]
    (.toLocalDate v))
  java.sql.Time
  (read-column-by-label [^java.sql.Time v _]
    (.toLocalTime v))
  (read-column-by-index [^java.sql.Time v _2 _3]
    (.toLocalTime v)))

(defn result-one-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-one this result options)
       (transform-keys ->kebab-case-keyword)))

(defn result-many-snake->kebab
  [this result options]
  (->> (hugsql.adapter/result-many this result options)
       (map #(transform-keys ->kebab-case-keyword %))))

(defmethod hugsql.core/hugsql-result-fn :1 [sym]
  'daisyproducer2.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :one [sym]
  'daisyproducer2.db.core/result-one-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :* [sym]
  'daisyproducer2.db.core/result-many-snake->kebab)

(defmethod hugsql.core/hugsql-result-fn :many [sym]
  'daisyproducer2.db.core/result-many-snake->kebab)

