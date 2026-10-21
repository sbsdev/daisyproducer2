(ns daisyproducer2.test.db.core-test
  (:require
   [daisyproducer2.db.core :refer [*db*] :as db]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [daisyproducer2.config :refer [env]]
   [daisyproducer2.whitelists.hyphenation :as hyphenations]
   [daisyproducer2.words.global :as global]
   [daisyproducer2.words.statistics :as statistics]
   [mount.core :as mount]))

(use-fixtures
  :once
  (fn [f]
    (mount/start
     #'daisyproducer2.config/env
     #'daisyproducer2.db.core/*db*)
    #_(migrations/migrate ["migrate"] (select-keys env [:database-url]))
    (f)))

(deftest ^:database test-global-words
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (is (= 1 (count (db/find-global-words t-conn {:limit 1 :offset 0 :search "hausboot"} {}))))
    #_(is (= 2 (count (db/get-confirmable-words t-conn {:limit 2 :offset 0} {}))))))

(deftest ^:database test-confirmable
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [confirmable (count (db/get-confirmable-words t-conn {:limit 10000 :offset 0} {}))
          ;; create our own document, so that the test does not depend on the contents of the db
          document-id (db/get-generated-key
                       (db/insert-document t-conn {:title "Test" :author "Test" :publisher "Test"
                                                   :date "2026-01-01" :identifier "test-confirmable"
                                                   :language "de"}))
          word {:untranslated "hahaha2" :contracted "H+H+H+2" :uncontracted "HAHAHA2"
                :type 0 :homograph-disambiguation "" :document-id document-id :islocal false}
          _ (db/insert-local-word t-conn word)
          ;; only words of closed documents can be confirmed
          _ (db/update-document-state t-conn {:id document-id :state "closed"})]
      (is (= (inc confirmable) (count (db/get-confirmable-words t-conn {:limit 10000 :offset 0} {})))))))

(deftest ^:database test-put-statistics
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [document-id (db/get-generated-key
                       (db/insert-document t-conn {:title "Test" :author "Test" :publisher "Test"
                                                   :date "2026-01-01" :identifier "test-put-statistics"
                                                   :language "de"}))
          rows #(jdbc/execute! t-conn ["SELECT grade, total, unknown FROM statistics_documentstatistic WHERE document_id = ? ORDER BY grade" document-id])]
      ;; a word that is in no dictionary
      (db/insert-unknown-words t-conn {:words [["xqzzyvorbelpf" 0 "" document-id]]})
      (binding [*db* t-conn]
        (is (= 2 (statistics/put-statistics document-id 1))))
      (is (= [{:grade 1 :total 1 :unknown 1} {:grade 2 :total 1 :unknown 1}]
             (map #(update-keys % (comp keyword name)) (rows))))
      (testing "only the first version is recorded"
        (binding [*db* t-conn]
          (is (= 0 (statistics/put-statistics document-id 42))))
        (is (= 2 (count (rows))))))))

(deftest ^:database test-delete-hyphenation
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [other-document-id (db/get-generated-key
                             (db/insert-document t-conn {:title "Test" :author "Test" :publisher "Test"
                                                         :date "2026-01-01" :identifier "test-delete-hyphenation"
                                                         :language "de"}))
          hyphenation {:word "xqzzyvorbelpf" :hyphenation "xqzzy-vor-belpf" :spelling 1}
          word {:untranslated "xqzzyvorbelpf" :uncontracted "XQZZYVORBELPF" :contracted "XQZZYVORBELPF"
                :type 0 :homograph-disambiguation ""}
          delete #(db/delete-hyphenation t-conn (dissoc hyphenation :hyphenation))]
      (db/insert-hyphenation t-conn hyphenation)
      (testing "the hyphenation is kept while a global word refers to it"
        (db/insert-global-word t-conn word)
        (is (= 0 (delete)))
        (db/delete-global-word t-conn word))
      (testing "the hyphenation is kept while a local word of another document refers to it"
        (db/insert-local-word t-conn (assoc word :document-id other-document-id :islocal false))
        (is (= 0 (delete)))
        (db/delete-local-word t-conn (assoc word :document-id other-document-id)))
      (testing "the hyphenation is deleted once no word refers to it any more"
        (is (= 1 (delete)))))))

(deftest ^:database test-delete-global-word
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [word {:untranslated "xqzzyvorbelpf" :uncontracted "XQZZYVORBELPF" :contracted "XQZZYVORBELPF"
                :type 0 :homograph-disambiguation ""}
          hyphenations #(:n (jdbc/execute-one! t-conn ["SELECT COUNT(*) AS n FROM hyphenation_words WHERE word = ?" (:untranslated word)]))]
      (with-redefs [hyphenations/export (constantly nil)]
        (binding [*db* t-conn]
          (doseq [spelling [0 1]]
            (db/insert-hyphenation {:word (:untranslated word) :hyphenation "xqzzy-vor-belpf" :spelling spelling}))
          (testing "the hyphenations are kept while a local word refers to them"
            (let [document-id (db/get-generated-key
                               (db/insert-document {:title "Test" :author "Test" :publisher "Test"
                                                    :date "2026-01-01" :identifier "test-delete-global-word"
                                                    :language "de"}))]
              (db/insert-local-word (assoc word :document-id document-id :islocal false))
              (db/insert-global-word word)
              (global/delete-word word)
              (is (= 2 (hyphenations)))
              (db/delete-local-word (assoc word :document-id document-id))))
          (testing "the hyphenations of both spellings are deleted with the last word"
            (db/insert-global-word word)
            (global/delete-word word)
            (is (= 0 (hyphenations)))))))))

(deftest ^:database test-unconfirm-non-local-word
  (jdbc/with-transaction [t-conn *db* {:rollback-only true}]
    (let [document-id (db/get-generated-key
                       (db/insert-document t-conn {:title "Test" :author "Test" :publisher "Test"
                                                   :date "2026-01-01" :identifier "test-unconfirm-non-local-word"
                                                   :language "de"}))
          word {:untranslated "xqzzyvorbelpf" :uncontracted "XQZZYVORBELPF" :contracted "XQZZYVORBELPF"
                :type 0 :homograph-disambiguation "" :document-id document-id}
          confirmed? #(:isConfirmed (jdbc/execute-one! t-conn ["SELECT isConfirmed FROM dictionary_localword WHERE untranslated = ? AND document_id = ?" (:untranslated word) document-id]
                                                       {:builder-fn next.jdbc.result-set/as-unqualified-maps}))]
      (db/insert-local-word t-conn (assoc word :islocal true :isconfirmed true))
      (testing "a local word stays confirmed when it is saved again"
        (db/insert-local-word t-conn (assoc word :islocal true))
        (is (true? (confirmed?))))
      (testing "a word that is saved as not local has to be confirmed again"
        (db/insert-local-word t-conn (assoc word :islocal false))
        (is (false? (confirmed?)))))))
