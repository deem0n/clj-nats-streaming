(defproject clj-nats-streaming "0.1.0-SNAPSHOT"
  :description "A Clojure library for NATS Streaming."
  :url "http://github.com/deem0n/clj-nats-streaming"
  :license {:name "MIT"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/tools.logging "0.4.0"]
                 [com.stuartsierra/component "0.3.2"]
                 [io.nats/java-nats-streaming "0.5.0"]]
  :main ^:skip-aot clj-nats-streaming.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
