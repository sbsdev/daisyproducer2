(ns daisyproducer2.documents.versions
  (:require [babashka.fs :as fs]
            [clojure.java.io :as io]
            [daisyproducer2.config :refer [env]]
            [daisyproducer2.db.core :as db]
            [daisyproducer2.metrics :as metrics]
            [daisyproducer2.documents.schema-validation :as schema-validation]
            [daisyproducer2.documents.metadata-validation :as metadata-validation]
            [iapetos.collector.fn :as prometheus]
            [clojure.tools.logging :as log]
            [daisyproducer2.pipeline1 :as pipeline1])
  ;; Universally Unique Lexicographically Sortable Identifiers (https://github.com/ulid/spec)
  (:import [io.azam.ulidj ULID] ))

(defn get-versions
  ([document-id]
   (db/get-versions {:document_id document-id}))
  ([document-id limit offset]
   (db/get-versions {:document_id document-id :limit limit :offset offset})))

(defn find-versions
  [document-id limit offset search]
  (db/find-versions {:document_id document-id :limit limit :offset offset :search search}))

(defn get-version
  [document-id id]
  (db/get-version {:document_id document-id :id id}))

(defn get-latest
  [document-id]
  (db/get-latest-version {:document_id document-id}))

(defn get-content
  [version]
  (let [document-root (env :document-root)
        path (:content version)]
    (io/file document-root path)))

(def ^:private schema "schema/dtbook-2005-3-sbs.rng")

(defn validate-version [file document]
  (concat
   (schema-validation/validation-errors file schema)
   (metadata-validation/validate-metadata file document)
   (pipeline1/validate file :dtbook)))

(defn insert-version
  [document-id tempfile comment user]
  (let [document-root (env :document-root)
        name (str (ULID/random) ".xml")
        path (fs/path (str document-id) "versions" name)
        absolute-path (fs/absolutize (fs/path document-root path))
        document (db/get-document {:id document-id})]
    ;; validate tempfile
    (let [validation-errors (validate-version tempfile document)]
      (log/debugf "Vaidating %s" tempfile)
      (when (seq validation-errors)
        (throw (ex-info "Failed to validate XML"
                        {:error-id :invalid-dtbook :errors validation-errors}))))
    ;; make sure path exists
    (fs/create-dirs (fs/parent absolute-path))
    ;; copy the contents into the archive
    (with-open [in (io/input-stream tempfile)
                out (io/output-stream (fs/file absolute-path))]
      (io/copy in out))
    ;; store it in the db ...
    (->
     (db/insert-version {:document_id document-id :comment comment :content (str path) :user user})
     ;; ... and return the new key
     db/get-generated-key)))

(defn version-path [version]
  (let [document-root (env :document-root)]
    (fs/path document-root (:content version))))

(defn delete-version
  "Delete a version given a `document-id` and an version `id`. Return the number of rows affected."
  [document-id id]
  ;; we need to fetch the version first to know the path to the xml file, which we will have to
  ;; delete also
  (if-let [version (db/get-version {:document_id document-id :id id})]
    (let [deletions (db/delete-version {:id id})]
      (when-not (fs/delete-if-exists (version-path version))
        ;; if a version file does not exist we simply log that fact,
        ;; but do not raise an exception
        (log/errorf "Attempting to delete non-existing version file %s" (version-path version)))
      deletions)
    0)) ;; since we could not find the version we'll return zero deletions

(defn delete-old-versions
  "Delete all but the latest version given a `document-id`. Return the number of rows affected."
  [document-id]
  ;; we need to fetch the versions first to know the path to the xml file, which we
  ;; will have to delete also
  (let [old-versions (rest (db/get-versions {:document_id document-id}))]
    (if (seq old-versions)
      (do
        (doseq [old-version old-versions]
          (when-not (fs/delete-if-exists (version-path old-version))
            ;; if an version file does not exist we simply log that fact, but do
            ;; not raise an exception
            (log/errorf "Attempting to delete non-existing version file %s" (version-path old-version))))
        (db/delete-old-versions {:document_id document-id}))
      0)))

(defn delete-old-versions-of-closed-documents
  "Delete all but the latest version of all closed documents. Return the number of rows affected."
  []
  ;; we need to fetch the versions first to know the path to the xml file, which we
  ;; will have to delete also
  (let [old-versions (db/get-old-versions-of-closed-documents {})]
    (if (seq old-versions)
      (do
        (doseq [old-version old-versions]
          (when-not (fs/delete-if-exists (version-path old-version))
            ;; if an version file does not exist we simply log that fact, but do
            ;; not raise an exception
            (log/errorf "Attempting to delete non-existing version file %s" (version-path old-version))))
        (db/delete-old-versions-of-closed-documents {}))
      0)))

(prometheus/instrument! metrics/registry #'get-versions)
(prometheus/instrument! metrics/registry #'get-version)
(prometheus/instrument! metrics/registry #'get-latest)
(prometheus/instrument! metrics/registry #'get-content)
(prometheus/instrument! metrics/registry #'insert-version)
(prometheus/instrument! metrics/registry #'delete-version)
(prometheus/instrument! metrics/registry #'delete-old-versions)
(prometheus/instrument! metrics/registry #'delete-old-versions-of-closed-documents)
