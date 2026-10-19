(ns daisyproducer2.test.db.core-test
  (:require
   [daisyproducer2.db.core :refer [*db*] :as db]
   [luminus-migrations.core :as migrations]
   [clojure.test :refer :all]
   [next.jdbc :as jdbc]
   [daisyproducer2.config :refer [env]]
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
