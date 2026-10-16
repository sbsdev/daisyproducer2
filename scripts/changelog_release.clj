;; Turn the [Unreleased] section of CHANGELOG.md into a section for the
;; version in project.clj. Run as part of `lein release`, after the version
;; has been bumped to the release version and before it is committed.
;;
;;   lein run -m clojure.main scripts/changelog_release.clj

(require '[clojure.string :as str])

(defn fail [& msg]
  (binding [*out* *err*] (println (apply str "changelog: " msg)))
  (System/exit 1))

(let [version (nth (read-string (slurp "project.clj")) 2)
      date (str (java.time.LocalDate/now))
      changelog (slurp "CHANGELOG.md")
      unreleased (second (re-find #"(?s)## \[Unreleased\]\n(.*?)\n## \[" changelog))
      link-re #"\[Unreleased\]: (.*/compare/)(.*)\.\.\.HEAD"
      [_ compare-url previous] (re-find link-re changelog)]
  (cond
    (str/ends-with? version "-SNAPSHOT") (fail "version " version " is not a release version")
    (str/includes? changelog (str "## [" version "]")) (fail "there is already a section for " version)
    (str/blank? unreleased) (fail "there are no unreleased changes to release")
    (nil? previous) (fail "cannot find the link for [Unreleased]"))
  (spit "CHANGELOG.md"
        (-> changelog
            (str/replace-first "## [Unreleased]\n"
                               (str "## [Unreleased]\n\n## [" version "] - " date "\n"))
            (str/replace-first link-re
                               (str "[Unreleased]: " compare-url version "...HEAD\n"
                                    "[" version "]: " compare-url previous "..." version))))
  (println "changelog: released" version))
