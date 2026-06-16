#!/usr/bin/env bb
;; Compare braille output between two daisyproducer2 servers.
;; Used to validate dtbook2sbsform upgrades.
;;
;; Usage:
;;   ./compare-braille.bb [CONFIG-FILE]
;;
;; CONFIG-FILE defaults to compare-braille-config.edn in the current directory.

(require '[babashka.fs :as fs]
         '[babashka.http-client :as http]

         '[cheshire.core :as json]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

;;; Braille parameter sets to test

(def base-params
  {:cells-per-line             28
   :lines-per-page             28
   :toc-level                  0
   :footer-level               0
   :include-macros             true
   :show-original-page-numbers true
   :show-v-forms               true
   :downshift-ordinals         true
   :enable-capitalization      false
   :footnote-placement         "standard"})

(def param-sets
  [;; 1: Kurzschrift (grade 2, most contracted)
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss"}
   ;; 2: Vollschrift (grade 1)
   {:contraction 1 :hyphenation false :detailed-accented-characters "swiss"}
   ;; 3: Basisschrift (grade 0)
   {:contraction 0 :hyphenation false :detailed-accented-characters "swiss"}
   ;; 4: contracted + hyphenation
   {:contraction 2 :hyphenation true  :detailed-accented-characters "swiss"}
   ;; 5: basic accents
   {:contraction 2 :hyphenation false :detailed-accented-characters "basic"}
   ;; 6: no original page numbers
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss" :show-original-page-numbers false}
   ;; 7: no v-forms
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss" :show-v-forms false}
   ;; 8: no downshift ordinals
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss" :downshift-ordinals false}
   ;; 9: capitalization enabled
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss" :enable-capitalization true}
   ;; 10: footnotes at end of volume
   {:contraction 2 :hyphenation false :detailed-accented-characters "swiss" :footnote-placement "end-vol"}])

(defn auth-headers [token]
  {"Authorization" token
   "Accept"        "application/json"})

(defn json-request [method base-url path token body]
  (http/request {:method  method
                 :uri     (str base-url path)
                 :headers (merge (auth-headers token) {"Content-Type" "application/json"})
                 :body    (json/generate-string body)}))

;;; HTTP helpers

(defn get-json [base-url path token]
  (let [resp (http/get (str base-url path)
                       {:headers (auth-headers token)})]
    (when (= 200 (:status resp))
      (json/parse-string (:body resp) true))))

(defn get-text [base-url path]
  (let [resp (http/get (str base-url path))]
    (when (= 200 (:status resp))
      (:body resp))))

;;; Finding documents with braille products

(defn braille-products? [base-url token doc-id]
  (seq (get-json base-url (str "/api/documents/" doc-id "/products?type=0") token)))

(defn all-documents [base-url token]
  (println "  fetching all documents...")
  (loop [offset 0 acc []]
    (let [batch (get-json base-url (str "/api/documents?limit=500&offset=" offset) token)]
      (if (empty? batch)
        (do (println (str "  " (count acc) " documents total")) acc)
        (recur (+ offset 500) (into acc batch))))))

(defn find-braille-documents [base-url token limit]
  (let [newest-first (sort-by :id > (all-documents base-url token))]
    (println "  checking for braille products, newest first...")
    (loop [candidates (partition-all 50 newest-first) found []]
      (cond
        (>= (count found) limit) (vec (take limit found))
        (empty? candidates)      (vec found)
        :else
        (let [qualifying (->> (first candidates)
                              (pmap #(when (braille-products? base-url token (:id %)) %))
                              (filter some?)
                              vec)]
          (recur (rest candidates) (into found qualifying)))))))

;;; Document & version operations

(defn get-latest-version [base-url doc-id]
  (some-> (get-text base-url (str "/api/documents/" doc-id "/versions?latest=true"))
          (json/parse-string true)))

(defn get-document [base-url token doc-id]
  (get-json base-url (str "/api/documents/" doc-id) token))

(defn patch-identifier [xml new-identifier]
  (-> xml
      (str/replace #"(<meta name=\"dc:Identifier\" content=\")[^\"]*(\")" (str "$1" new-identifier "$2"))
      (str/replace #"(<meta name=\"dtb:uid\" content=\")[^\"]*(\")"       (str "$1" new-identifier "$2"))))

(defn download-xml [base-url version-content]
  (get-text base-url (str "/archive/" version-content)))

(defn delete-document [base-url token doc-id]
  (try
    (http/delete (str base-url "/api/documents/" doc-id)
                 {:headers (auth-headers token)})
    (catch Exception e
      (println (str "    WARNING: failed to delete test doc " doc-id ": " (.getMessage e))))))

(defn create-document [base-url token doc]
  (let [body (->> (select-keys doc [:title :author :publisher :date :language
                                    :subject :description :source :source-date
                                    :source-edition :source-publisher :source-rights
                                    :production-series :production-series-number
                                    :production-source])
                  (remove (comp nil? val))
                  (into {}))
        resp (json-request :post base-url "/api/documents" token body)]
    (if (= 201 (:status resp))
      (-> resp :headers (get "location") (str/split #"/") last parse-long)
      (throw (ex-info (str "Failed to create document: HTTP " (:status resp))
                      {:status (:status resp) :body (:body resp)})))))

(defn upload-version [base-url token doc-id xml-content filename]
  (let [tmp (fs/create-temp-file {:suffix ".xml"})]
    (try
      (spit (fs/file tmp) xml-content)
      (http/post (str base-url "/api/documents/" doc-id "/versions")
                 {:headers   (auth-headers token)
                  :multipart [{:name         "file"
                               :content      (fs/file tmp)
                               :filename     filename
                               :content-type "application/xml"}
                              {:name    "comment"
                               :content "Imported for braille regression test"}]})
      (finally
        (fs/delete tmp)))))

;;; Local words

(defn get-local-words [base-url token doc-id]
  (loop [offset 0 acc []]
    (let [batch (get-json base-url
                          (str "/api/documents/" doc-id "/words?grade=0&limit=500&offset=" offset)
                          token)]
      (if (empty? batch)
        acc
        (recur (+ offset 500) (into acc batch))))))

(defn put-local-word [base-url token test-doc-id word]
  (let [body (-> (select-keys word [:untranslated :type :homograph-disambiguation
                                    :islocal :spelling :uncontracted :contracted :hyphenated])
                 (assoc :document-id test-doc-id))]
    (json-request :put base-url (str "/api/documents/" test-doc-id "/words") token body)))

(defn upload-local-words [prod-url prod-token test-url test-token prod-doc-id test-doc-id]
  (let [words (get-local-words prod-url prod-token prod-doc-id)]
    (when (seq words)
      (println (str "    uploading " (count words) " local words..."))
      (doseq [word words]
        (put-local-word test-url test-token test-doc-id word))
      (println "    waiting 15s for whitelist export...")
      (Thread/sleep 15000))))

;;; Braille generation

(defn param-query-string [param-set]
  (->> (merge base-params param-set)
       (map (fn [[k v]] (str (name k) "=" v)))
       (str/join "&")))

(defn generate-braille [base-url token doc-id param-set]
  (let [resp (http/get (str base-url "/api/documents/" doc-id "/preview/braille?"
                            (param-query-string param-set))
                       {:headers (auth-headers token)})]
    (when (= 201 (:status resp))
      (-> resp :body (json/parse-string true) :location))))

(defn download-braille [base-url location]
  (get-text base-url location))

;;; Per-document processing

(defn process-doc [prod-url prod-token test-url test-token output-dir doc]
  (let [doc-id (:id doc)]
    (try
      (let [version (get-latest-version prod-url doc-id)]
        (if-not version
          {:doc-id doc-id :error "no version found"}
          (let [version-content (:content version)
                xml-content     (download-xml prod-url version-content)
                xml-filename    (last (str/split version-content #"/"))
                test-doc-id     (create-document test-url test-token doc)
                test-identifier (:identifier (get-document test-url test-token test-doc-id))
                xml-content     (patch-identifier xml-content test-identifier)]
            (try
              (Thread/sleep (rand-int 5000))
              (upload-version test-url test-token test-doc-id xml-content xml-filename)
              (upload-local-words prod-url prod-token test-url test-token doc-id test-doc-id)
              (let [prod-identifier (:identifier doc)
                    normalize       #(-> % (str/replace prod-identifier "IDENTIFIER")
                                           (str/replace test-identifier "IDENTIFIER"))
                    results
                    (mapv
                     (fn [i param-set]
                       (println (str "    param-set " i "/" (count param-sets) " generating..."))
                       (let [prod-loc (generate-braille prod-url prod-token doc-id param-set)
                             _        (println (str "    param-set " i "/" (count param-sets) " prod done, generating test..."))
                             test-loc (generate-braille test-url test-token test-doc-id param-set)]
                         (if (and prod-loc test-loc)
                           (let [prod-bk   (normalize (download-braille prod-url prod-loc))
                                 test-bk   (normalize (download-braille test-url test-loc))
                                 prod-file (io/file output-dir "prod" (str doc-id "-" i ".bk"))
                                 test-file (io/file output-dir "test" (str doc-id "-" i ".bk"))]
                             (println (str "    param-set " i "/" (count param-sets) " done"))
                             (io/make-parents prod-file)
                             (io/make-parents test-file)
                             (spit prod-file prod-bk)
                             (spit test-file test-bk)
                             {:doc-id doc-id :param-set i :identical? (= prod-bk test-bk)})
                           {:doc-id doc-id :param-set i :error "braille generation failed"})))
                     (range 1 (inc (count param-sets)))
                     param-sets)]
                {:doc-id doc-id :results results})
              (finally
                (delete-document test-url test-token test-doc-id))))))
      (catch clojure.lang.ExceptionInfo e
        {:doc-id doc-id :error (str (ex-message e) " — " (pr-str (ex-data e)))})
      (catch Exception e
        {:doc-id doc-id :error (.getMessage e)}))))

;;; Summary report

(defn print-summary [doc-results]
  (let [all-results (mapcat :results (filter :results doc-results))
        doc-errors  (filter :error (filter (complement :results) doc-results))
        silent-fails (filter #(and (nil? (:results %)) (nil? (:error %))) doc-results)
        run-errors  (filter :error all-results)
        identical   (filter :identical? all-results)
        different   (remove #(or (:error %) (:identical? %)) all-results)]
    (println "\n=== Braille Comparison Results ===")
    (println (format "Documents processed : %d" (count doc-results)))
    (println (format "Param-set runs      : %d" (count all-results)))
    (println (format "  Identical         : %d" (count identical)))
    (println (format "  Different         : %d" (count different)))
    (println (format "  Errors            : %d" (+ (count doc-errors) (count run-errors) (count silent-fails))))
    (when (seq different)
      (println "\nDiffering files (prod/ vs test/):")
      (doseq [{:keys [doc-id param-set]} (sort-by (juxt :doc-id :param-set) different)]
        (println (format "  doc-id=%-8d param-set=%d" doc-id param-set))))
    (when (seq doc-errors)
      (println "\nDocument-level errors:")
      (doseq [{:keys [doc-id error]} doc-errors]
        (println (format "  doc-id=%-8d %s" doc-id error))))
    (when (seq silent-fails)
      (println "\nSilent failures (no result, no error — likely uncaught exception in pmap):")
      (doseq [r silent-fails]
        (println (format "  %s" (pr-str r)))))
    (when (seq run-errors)
      (println "\nGeneration errors:")
      (doseq [{:keys [doc-id param-set error]} run-errors]
        (println (format "  doc-id=%-8d param-set=%d  %s" doc-id param-set error))))))

;;; Config & entry point

(def default-config-file "compare-braille-config.edn")

(def usage
  (str/join \newline
            ["Usage: compare-braille.bb [CONFIG-FILE]"
             ""
             "CONFIG-FILE defaults to compare-braille-config.edn in the current directory."
             ""
             "Example config file:"
             ""
             "  {:prod-url   \"https://prod.sbszh.ch\""
             "   :test-url   \"https://test.sbszh.ch\""
             "   :token      \"Token <your-jwt-token>\""
             "   :output-dir \"/tmp/braille-cmp\"  ; optional, this is the default"
             "   :limit      100}                 ; optional, this is the default"
             ""
             "Obtain a token via POST /api/login. The config file is gitignored."]))

(when (some #{"--help" "-h"} *command-line-args*)
  (println usage)
  (System/exit 0))

(defn load-config [path]
  (let [f (io/file path)]
    (when-not (.exists f)
      (println (str "Config file not found: " path))
      (println)
      (println usage)
      (System/exit 1))
    (edn/read-string (slurp f))))

(let [{:keys [prod-url test-url token output-dir limit document-ids]
       :or   {output-dir "/tmp/braille-cmp" limit 100}}
      (load-config (or (first *command-line-args*) default-config-file))]
  (when-not (and prod-url test-url token)
    (println "Error: config must contain :prod-url, :test-url and :token")
    (System/exit 1))
  (let [docs (if document-ids
               (do (println (str "Fetching " (count document-ids) " specific documents..."))
                   (mapv #(get-document prod-url token %) document-ids))
               (do (println (str "Finding up to " limit " documents with braille products..."))
                   (find-braille-documents prod-url token limit)))]
    (println (str "Processing " (count docs) " documents..."))
    (let [results (->> docs
                       (partition-all 10)
                       (mapcat (fn [batch]
                                 (pmap #(do (println (str "  doc " (:id %) " – " (:title %)))
                                            (process-doc prod-url token test-url token output-dir %))
                                       batch)))
                       vec)]
      (print-summary results))))
