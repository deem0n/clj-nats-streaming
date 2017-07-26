(ns clj-nats-streaming.core-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clj-nats-streaming.core :refer :all]))

(def system (component/system-map
             :connection
             (clj-nats-streaming.core/connection "test-cluster" "clojure-nats-client" nil)
             :foo-sub
             (component/using
              (clj-nats-streaming.core/subscription
               "1.1"
               [#(do (println "Subject:" (.getSubject %) "Payload:" (String. (.getData %))) (log/infof "To this I say \"foo\": %s" (String. (.getData %))))])
              [:connection])
              ))

(defn start-stan []
  (alter-var-root #'system component/start))

(deftest a-test
  (testing "Connect to the NATS streaming server"
    (is (start-stan))))

