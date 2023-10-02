(defproject ch.sbs/daisyproducer2 "0.8.23-SNAPSHOT"

  :description "FIXME: write description"
  :url "https://github.com/sbsdev/daisyproducer2"

  :dependencies [[babashka/fs "0.4.19"]
                 [babashka/process "0.5.21"]
                 [buddy/buddy-auth "3.0.323"]
                 [buddy/buddy-core "1.11.423"]
                 [buddy/buddy-hashers "2.0.167"]
                 [buddy/buddy-sign "3.5.351"]
                 [ch.qos.logback/logback-classic "1.4.8"]
                 [camel-snake-kebab "0.4.3"]
                 [cheshire "5.11.0" :exclusions [com.fasterxml.jackson.core/jackson-core]]
                 [clj-commons/iapetos "0.1.13" :exclusions [javax.xml.bind/jaxb-api]]
                 [clj-http "3.12.3" :exclusions [commons-io]] ;; to talk to the DAISY Pipeline2
                 [cljs-ajax "0.8.4"]
                 [clojure.java-time "1.2.0"]
                 [com.cognitect/transit-clj "1.0.333"]
                 [com.cognitect/transit-cljs "0.8.280"]
                 [com.google.javascript/closure-compiler-unshaded "v20230411"]
                 ;; [com.fasterxml.jackson.core/jackson-core "2.12.5"]
                 ;; [com.fasterxml.jackson.core/jackson-databind "2.12.5"]
                 [com.google.protobuf/protobuf-java "3.23.3"]
                 [com.taoensso/tempura "1.5.3"]
                 [conman "0.9.6"]
                 [cprop "0.1.19"]
                 [crypto-random "1.2.1"] ; for pipeline2 client
                 [day8.re-frame/http-fx "0.2.4"]
                 [expound "0.9.0"]
                 [funcool/struct "1.4.0"]
                 [io.prometheus/simpleclient_hotspot "0.12.0"]
                 [jarohen/chime "0.3.3"]
                 [luminus-undertow "0.1.18"]
                 [luminus/ring-ttl-session "0.3.3"]
                 [luminus-migrations "0.7.5"]
                 [luminus-transit "0.1.6"]
                 [markdown-clj "1.11.4"]
                 [me.flowthing/sigel "1.0.3"]
                 [metosin/jsonista "0.3.7"]
                 [metosin/muuntaja "0.6.8"]
                 [metosin/reitit "0.6.0"]
                 [metosin/ring-http-response "0.9.3"]
                 [mount "0.1.17"]
                 [mysql/mysql-connector-java "8.0.33"]
                 [nrepl "1.0.0"]
                 [org.clojars.pntblnk/clj-ldap "0.0.17"]
                 [org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60" :scope "provided"]
                 [org.clojure/core.async "1.5.648"]
                 [org.clojure/data.codec "0.1.1"] ; to talk to the DAISY Pipeline2
                 [org.clojure/data.zip "1.0.0"] ; to talk to the DAISY Pipeline2
                 [org.clojure/tools.cli "1.0.219"]
                 [org.clojure/tools.logging "1.2.4"]
                 [org.daisy.bindings/jhyphen "1.0.2" :exclusions [net.java.dev.jna/jna]]
                 [org.liblouis/liblouis-java "5.0.2" :exclusions [net.java.dev.jna/jna]]
                 [org.tobereplaced/nio.file "0.4.0"]
                 [org.webjars.npm/bulma "0.9.4"]
                 [org.webjars.npm/creativebulma__bulma-tooltip "1.0.2"]
                 [org.webjars.npm/material-icons "1.13.2"]
                 [org.webjars/webjars-locator "0.46"]
                 [org.webjars/webjars-locator-jboss-vfs "0.1.0"]
                 [pandect "1.0.2"] ;; to talk to the DAISY Pipeline2
                 [re-frame "1.3.0"]
                 [reagent "1.2.0"]
                 [ring-webjars "0.2.0"]
                 [ring/ring-core "1.10.0"]
                 [ring/ring-defaults "0.3.4"]
                 [selmer "1.12.58"]
                 [thheller/shadow-cljs "2.25.7" :scope "provided"]
                 [trptcolin/versioneer "0.2.0"]]

  :min-lein-version "2.0.0"
  
  :source-paths ["src/clj" "src/cljs" "src/cljc"]
  :test-paths ["test/clj"]
  :resource-paths ["resources" "target/cljsbuild"]
  :target-path "target/%s/"
  :main ^:skip-aot daisyproducer2.core

  :plugins []

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

  :clean-targets ^{:protect false}
  [:target-path "target/cljsbuild"]


  :test-selectors {:non-database (complement :database)}

  :profiles
  {:uberjar {:omit-source true

             :prep-tasks ["compile" ["run" "-m" "shadow.cljs.devtools.cli" "release" "app"]]
             :aot :all
             :uberjar-name "daisyproducer2.jar"
             :source-paths ["env/prod/clj"  "env/prod/cljs" ]
             :resource-paths ["env/prod/resources"]}

   :dev           [:project/dev :profiles/dev]
   :test          [:project/dev :project/test :profiles/test]

   :project/dev  {:jvm-opts ["-Dconf=dev-config.edn" ]
                  :dependencies [[binaryage/devtools "1.0.7"]
                                 [cider/piggieback "0.5.3"]
                                 [org.clojure/tools.namespace "1.4.4"]
                                 [pjstadig/humane-test-output "0.11.0"]
                                 [prone "2021-04-23"]
                                 [re-frisk "1.6.0"]
                                 [ring/ring-devel "1.10.0"]
                                 [ring/ring-mock "0.4.0"]]
                  :plugins      [[com.jakemccrary/lein-test-refresh "0.24.1"]
                                 [jonase/eastwood "1.2.4"]
                                 [cider/cider-nrepl "0.37.0"]]
                  
                  
                  :source-paths ["env/dev/clj"  "env/dev/cljs" "test/cljs" ]
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
