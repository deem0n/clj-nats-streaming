(ns clj-nats-streaming.core-test
  (:require [com.stuartsierra.component :as component]
            [clojure.test :refer :all]
            [clojure.tools.logging :as log]
            [clj-nats-streaming.core :refer :all]))


(def system (component/system-map
             :connection
             (clj-nats-streaming.core/connection "test-cluster" "clojure-nats-client" {:serverUrl "nats://localhost:4223"})

             :foo-sub
             (component/using
              (clj-nats-streaming.core/subscription
               "lvrz"
               [#(println "Subject:" (.getSubject %) "Payload:" (String. (.getData %)))
                #(log/infof "To this I say \"foo\": %s" (String. (.getData %)))]
               [])
              [:connection])
              ))

(defn start-stan []
  (do
    (alter-var-root #'system component/start)
    (Thread/sleep 60000)))


(deftest a-test
  (testing "Connect to the NATS streaming server"
    (is (start-stan))))

