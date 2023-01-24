(defproject ch.sbs/daisyproducer2 "0.8.17-SNAPSHOT"

  :description "FIXME: write description"
  :url "https://github.com/sbsdev/daisyproducer2"

  :dependencies [[babashka/fs "0.0.5"]
                 [buddy/buddy-auth "3.0.1"]
                 [buddy/buddy-core "1.10.1"]
                 [buddy/buddy-hashers "1.8.1"]
                 [buddy/buddy-sign "3.4.1"]
                 [ch.qos.logback/logback-classic "1.2.6"]
                 [camel-snake-kebab "0.4.2"]
                 [cheshire "5.10.1"]
                 [clj-commons/iapetos "0.1.11"]
                 [cljs-ajax "0.8.4"]
                 [clojure.java-time "0.3.3"]
                 [com.cognitect/transit-clj "1.0.324"]
                 [com.fasterxml.jackson.core/jackson-core "2.12.5"]
                 [com.fasterxml.jackson.core/jackson-databind "2.12.5"]
                 [com.google.protobuf/protobuf-java "3.18.0"]
                 [com.taoensso/tempura "1.2.1"]
                 [conman "0.9.1"]
                 [cprop "0.1.17"]
                 [day8.re-frame/http-fx "0.2.2"]
                 [expound "0.8.9"]
                 [funcool/struct "1.4.0"]
                 [io.prometheus/simpleclient_hotspot "0.12.0"]
                 [jarohen/chime "0.3.3"]
                 [luminus-undertow "0.1.12"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [luminus-migrations "0.7.1"]
                 [luminus-transit "0.1.2"]
                 [markdown-clj "1.10.6"]
                 [me.flowthing/sigel "1.0.1"]
                 [metosin/jsonista "0.3.4"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.5.15"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.16"]
                 [mysql/mysql-connector-java "8.0.23"]
                 [nrepl "0.8.3"]
                 [org.clojars.pntblnk/clj-ldap "0.0.17"]
                 [org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.773" :scope "provided"]
                 [org.clojure/core.async "1.3.610"]
                 [org.clojure/tools.cli "1.0.206"]
                 [org.clojure/tools.logging "1.1.0"]
                 [org.daisy.bindings/jhyphen "1.0.2"]
                 [org.liblouis/liblouis-java "4.3.1"]
                 [org.tobereplaced/nio.file "0.4.0"]
                 [org.webjars.npm/bulma "0.9.1"]
                 [org.webjars.npm/creativebulma__bulma-tooltip "1.0.2"]
                 [org.webjars.npm/material-icons "0.3.1"]
                 [org.webjars/webjars-locator "0.40"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [re-frame "1.2.0"]
                 [reagent "1.0.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.9.1"]
                 [ring/ring-defaults "0.3.2"]
                 [selmer "1.12.33"]
                 [thheller/shadow-cljs "2.20.3" :scope "provided"]
                 [trptcolin/versioneer "0.2.0"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot daisyproducer2.core

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag"]
                  #_["deploy"]
                  #_["uberjar"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  :plugins []
  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]

  :profiles
  {:uberjar {:omit-source true
             :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
             :aot :all
             :uberjar-name "daisyproducer2.jar"
             :source-paths ["env/prod/clj" "env/prod/cljs"]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[binaryage/devtools "1.0.6"]
                                 [cider/piggieback "0.5.3"]
                                 [org.clojure/tools.namespace "1.3.0"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [re-frisk "1.6.0"]
                                 [ring/ring-devel "1.9.6"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "1.2.4"]]
                  
                  :source-paths ["env/dev/clj" "env/dev/cljs" "test/cljs"]
                  :resource-paths ["env/dev/resources"]
                  :repl-options {:init-ns user
                                 :timeout 120000}
                  :injections [(require 'pjstadig.humane-test-output)
                               (pjstadig.humane-test-output/activate!)]}
   :project/test {:jvm-opts ["-Dconf=test-config.edn" ]
                  :resource-paths ["env/test/resources"] 
                  
                  }
   :profiles/dev {}
   :profiles/test {}})
